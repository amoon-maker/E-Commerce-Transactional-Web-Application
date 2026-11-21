import { useEffect, useState } from "react";
import {
  Badge,
  Button,
  Card,
  Col,
  Container,
  Form,
  Navbar,
  Nav,
  Row,
} from "react-bootstrap";

import logo from "./assets/umhamzalogo.png";
import familyQuantityImage from "./assets/products/FamilyQuantityOffer.png";
import apricotJamImage from "./assets/products/ApricotJam.png";
import cheeseNabulsiImage from "./assets/products/cheese nabulsi.png";
import frozenSambousekImage from "./assets/products/Frozen Sambousek.png";
import greenOlivesImage from "./assets/products/Green Olives.png";
import labanaShattaImage from "./assets/products/Labana Shatta.png";
import mintImage from "./assets/products/Mint.png";
import mixedAcharImage from "./assets/products/Mixed Achar.png";
import sumacBaladiImage from "./assets/products/Sumac Baladi.png";
import walnutSaladImage from "./assets/products/WalnutSalad.png";
import zatarBaladiImage from "./assets/products/Zatar Baladi.png";
import groupImage from "./assets/groupummhamza.png";

import "./App.css";
import Auth from "./Auth";
import CatalogManagementPanel from "./CatalogManagementPanel";

type Category =
  | "All Products"
  | "Dairy & Cheese"
  | "Traditional Pantry"
  | "Herbs & Spices";

type Product = {
  id: number;
  vendorId: number;
  name: string;
  category: Exclude<Category, "All Products">;
  description: string;
  price: number;
  image: string | null;
  stockQuantity: number;
};

type BackendProduct = {
  productId: number;
  vendorId: number;
  sku: string;
  productName: string;
  description: string | null;
  price: number;
  salePrice: number | null;
  packageSize: number | null;
  packageUnit: string | null;
  active: boolean;
  stockQuantity: number;
  reservedQuantity: number;
  status: string;
  variants: {
    variantId: number;
    sku: string;
    variantName: string;
    price: number;
    salePrice: number | null;
    stockQuantity: number;
    active: boolean;
  }[];
};

type CartItem = {
  product: Product;
  quantity: number;
};

type CartApiItem = {
  cartItemId: number;
  productId: number;
  vendorId: number;
  sku: string;
  productName: string;
  description: string | null;
  unitPrice: number;
  quantity: number;
  availableStock: number;
  active: boolean;
};

type CartApiResponse = { items: CartApiItem[] };

type FavoriteApiItem = {
  productId: number;
  sku: string;
  productName: string;
  unitPrice: number;
  availableStock: number;
  active: boolean;
};

type OrderSummary = {
  orderId: number;
  orderNumber: string;
  orderDate: string;
  status: string;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  shippingCost: number;
  totalAmount: number;
  invoiceNumber: string | null;
};

type OrderDetail = Omit<OrderSummary, "invoiceNumber"> & {
  invoiceNumber?: string | null;
  deliveryAddress: {
    addressLabel: string | null;
    addressLine1: string;
    addressLine2: string | null;
    city: string;
    emirate: string;
    postalCode: string | null;
    country: string;
    addressType: string;
  };
  items: {
    productId: number;
    sku: string;
    productName: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
  }[];
  invoice: {
    invoiceNumber: string;
    issueDate: string;
    subtotal: number;
    discountAmount: number;
    taxAmount: number;
    shippingCost: number;
    totalAmount: number;
    status: string;
  } | null;
  payment: {
    paymentMethod: string;
    status: string;
    authorizedAmount: number;
    capturedAmount: number;
    currencyCode: string;
  } | null;
  delivery: {
    status: string;
    deliveryMethod: string | null;
    estimatedDeliveryDate: string | null;
    actualDeliveryDate: string | null;
    notes: string | null;
  } | null;
  statusHistory: {
    status: string;
    comment: string | null;
    changedAt: string;
  }[];
};

const apiBaseUrl = "http://localhost:8080";

async function customerApiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const method = options.method?.toUpperCase() ?? "GET";
  const headers = new Headers(options.headers);

  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrfResponse = await fetch(`${apiBaseUrl}/api/auth/csrf`, {
      credentials: "include",
    });
    if (!csrfResponse.ok) {
      throw new Error("Unable to obtain security token.");
    }

    const csrfData: { headerName: string; token: string } =
      await csrfResponse.json();
    const csrfCookie = document.cookie
      .split(";")
      .map((cookie) => cookie.trim())
      .find((cookie) => cookie.startsWith("XSRF-TOKEN="));
    const csrfToken = csrfCookie
      ? decodeURIComponent(csrfCookie.substring("XSRF-TOKEN=".length))
      : csrfData.token;
    headers.set(csrfData.headerName, csrfToken);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    method,
    headers,
    credentials: "include",
  });

  if (!response.ok) {
    if (response.status === 401) {
      window.dispatchEvent(new Event("app-session-expired"));
    }
    let message = "The request could not be completed.";
    const responseText = await response.text();
    try {
      const errorBody = JSON.parse(responseText);
      message = errorBody.message ?? errorBody.detail ?? message;
    } catch {
      if (responseText) message = responseText;
    }
    throw new Error(message);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

function categoryForSku(sku: string): Exclude<Category, "All Products"> {
  if (["UH-CHS-500", "UH-LBS-500"].includes(sku)) return "Dairy & Cheese";
  if (["UH-ZTR-250", "UH-SUM-500", "LP-MNT-100"].includes(sku)) {
    return "Herbs & Spices";
  }
  return "Traditional Pantry";
}

function mapCartItems(data: CartApiResponse): CartItem[] {
  return data.items.map((item) => ({
    product: {
      id: item.productId,
      vendorId: item.vendorId,
      name: item.productName,
      category: categoryForSku(item.sku),
      description: item.description ?? "Product details are unavailable.",
      price: Number(item.unitPrice),
      image: productImagesBySku[item.sku] ?? null,
      stockQuantity: item.active ? item.availableStock : 0,
    },
    quantity: item.quantity,
  }));
}

const formatPrice = (amount: number) => {
  return new Intl.NumberFormat("en-AE", {
    style: "currency",
    currency: "AED",
  }).format(amount);
};

const displayOrderStatus = (status: string) =>
  (status === "OUT_FOR_DELIVERY" ? "SHIPPED" : status.replaceAll("_", " "));

const productImagesBySku: Record<string, string> = {
  "UH-CHS-500": cheeseNabulsiImage,
  "UH-JAM-400": apricotJamImage,
  "UH-PKL-1000": mixedAcharImage,
  "UH-SMB-500": frozenSambousekImage,
  "UH-ZTR-250": zatarBaladiImage,
  "LP-OLV-500": greenOlivesImage,
  "LP-MNT-100": mintImage,
  "UH-LBS-500": labanaShattaImage,
  "UH-SUM-500": sumacBaladiImage,
  "UH-WAL-500": walnutSaladImage,
  "UH-OFR-001": familyQuantityImage,
};

function ProductImage({
  image,
  alt,
  imageClassName,
  placeholderClassName,
}: {
  image: string | null;
  alt: string;
  imageClassName: string;
  placeholderClassName: string;
}) {
  const [imageFailed, setImageFailed] = useState(false);

  if (!image || imageFailed) {
    return (
      <div
        className={placeholderClassName}
        role="img"
        aria-label={`${alt}: image unavailable`}
      >
        <span>Image unavailable</span>
      </div>
    );
  }

  return (
    <img
      src={image}
      alt={alt}
      className={imageClassName}
      onError={() => setImageFailed(true)}
    />
  );
}

function App() {
  const [selectedCategory, setSelectedCategory] =
    useState<Category>("All Products");
  const [searchTerm, setSearchTerm] = useState("");

  const [products, setProducts] = useState<Product[]>([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [productsError, setProductsError] = useState("");
  const [catalogReloadKey, setCatalogReloadKey] = useState(0);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [cartBusy, setCartBusy] = useState(false);
  const [cartError, setCartError] = useState("");
  const [favorites, setFavorites] = useState<Set<number>>(new Set());
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [favoritesBusy, setFavoritesBusy] = useState(false);
  const [favoritesError, setFavoritesError] = useState("");
  const [cartOpen, setCartOpen] = useState(false);
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [orderConfirmed, setOrderConfirmed] = useState(false);
  const [orderLoading, setOrderLoading] = useState(false);
  const [orderError, setOrderError] = useState("");
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState("");
  const [vendorOrders, setVendorOrders] = useState<OrderDetail[]>([]);
  const [vendorOrdersOpen, setVendorOrdersOpen] = useState(false);
  const [vendorOrdersLoading, setVendorOrdersLoading] = useState(false);
  const [vendorOrdersError, setVendorOrdersError] = useState("");
  const [vendorActionBusy, setVendorActionBusy] = useState(false);
  const [vendorWorkspaceOpen, setVendorWorkspaceOpen] = useState(false);
  const [adminManagementOpen, setAdminManagementOpen] = useState(false);
  const [orderHistoryOpen, setOrderHistoryOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<OrderDetail | null>(null);
  const [orderDetailLoading, setOrderDetailLoading] = useState(false);

  const [paymentMethod, setPaymentMethod] = useState("CREDIT");

  const [createdOrder, setCreatedOrder] = useState<OrderDetail | null>(null);
  const [darkMode, setDarkMode] = useState(false);

  const [loggedInUser, setLoggedInUser] = useState<{
    email: string;
    roles: string[];
    vendorId?: number;
  } | null>(null);
  const [sessionChecked, setSessionChecked] = useState(false);
  const customerEmail = loggedInUser?.roles.includes("ROLE_CUSTOMER")
    ? loggedInUser.email
    : null;
  const canManageVendorOrders = Boolean(loggedInUser?.roles.some(
    (role) => role === "ROLE_VENDOR" || role === "ROLE_ADMIN",
  ));
  const isVendor = Boolean(loggedInUser?.roles.includes("ROLE_VENDOR"));
  const isAdmin = Boolean(loggedInUser?.roles.includes("ROLE_ADMIN"));

  const [authModalOpen, setAuthModalOpen] = useState(false);

  const [customer, setCustomer] = useState({
    address: "",
    city: "",
    emirate: "",
    postalCode: "",
  });

  function scrollToNavigationSection(sectionId: string) {
    window.requestAnimationFrame(() => {
      document.getElementById(sectionId)?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    });
  }

  // Scroll to cart when it opens
  useEffect(() => {
    if (cartOpen) {
      document.getElementById("shopping-cart")?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    }
  }, [cartOpen]);

  // Load products from backend
  useEffect(() => {
    fetch(`${apiBaseUrl}/api/products`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load products");
        }

        return response.json();
      })
      .then((data: BackendProduct[]) => {
        const activeProducts = data.filter(
          (product) =>
            product.active &&
            product.stockQuantity - (product.reservedQuantity ?? 0) > 0 &&
            product.sku !== "VENDOR-AUTH-TEST-20261117",
        );
        const mappedProducts: Product[] = activeProducts.map((product) => {
          let category: Exclude<Category, "All Products"> =
            "Traditional Pantry";
          const image = productImagesBySku[product.sku] ?? null;
          let description =
            product.description ?? "Product details are unavailable.";

          switch (product.sku) {
            case "UH-CHS-500":
              category = "Dairy & Cheese";
              description =
                "Traditional Akkawi cheese, packed in a convenient 500g size.";
              break;

            case "UH-PKL-1000":
              description =
                "A traditional mix of pickled vegetables, packed in a 1kg jar.";
              break;

            case "UH-ZTR-250":
              category = "Herbs & Spices";
              description =
                "A classic zaatar blend, bringing traditional flavour to your table.";
              break;

            case "UH-JAM-400":
              description =
                "Enjoy the fruity sweetness of apricot jam, packed in a 400g jar.";
              break;

            case "UH-SMB-500":
              description =
                "Traditional frozen cheese sambousek, ready to prepare and enjoy.";
              break;

            case "UH-OFR-001":
              description =
                "A convenient family-sized offer for sharing and enjoying together.";
              break;

            case "UH-LBS-500":
              category = "Dairy & Cheese";
              description = "Labana Shatta, 500g.";
              break;

            case "UH-SUM-500":
              category = "Herbs & Spices";
              description = "Sumac Baladi, 500g.";
              break;

            case "UH-WAL-500":
              description = "Walnut Salad, 500g.";
              break;

            case "LP-OLV-500":
              description = "Green Olives, 500g sample product.";
              break;

            case "LP-MNT-100":
              category = "Herbs & Spices";
              description = "Dried Mint, 100g sample product.";
              break;
          }

          return {
            id: product.productId,
            vendorId: product.vendorId,
            name: product.productName,
            category,
            description,
            price: product.salePrice ?? product.price,
            image,
            stockQuantity: Math.max(
              0,
              product.stockQuantity - (product.reservedQuantity ?? 0),
            ),
          };
        });

        setProducts(mappedProducts);
        setProductsError("");
      })
      .catch((error) => {
        console.error("Error loading products:", error);
        setProductsError("Unable to load products. Please try again later.");
      })
      .finally(() => {
        setProductsLoading(false);
      });
  }, [catalogReloadKey]);

  // Drop role-specific and customer-owned data as soon as the session expires.
  useEffect(() => {
    const clearExpiredSession = () => {
      setLoggedInUser(null);
      setCart([]);
      setFavorites(new Set());
      setFavoritesOnly(false);
      setOrders([]);
      setSelectedOrder(null);
      setVendorOrders([]);
      setVendorOrdersOpen(false);
      setVendorWorkspaceOpen(false);
      setAdminManagementOpen(false);
      setOrderHistoryOpen(false);
      setCartOpen(false);
      setCheckoutOpen(false);
      setCartError("");
      setFavoritesError("");
      setOrdersError("");
      setVendorOrdersError("");
      setOrderError("");
      setAuthModalOpen(false);
    };
    window.addEventListener("app-session-expired", clearExpiredSession);
    return () => window.removeEventListener("app-session-expired", clearExpiredSession);
  }, []);

  // Restore an existing authenticated session after a page refresh.
  useEffect(() => {
    let active = true;
    fetch(`${apiBaseUrl}/api/auth/me`, { credentials: "include" })
      .then(async (response) => {
        if (!response.ok) return null;
        return response.json();
      })
      .then((user) => {
        if (active && user) setLoggedInUser(user);
      })
      .catch(() => undefined)
      .finally(() => {
        if (active) setSessionChecked(true);
      });

    return () => {
      active = false;
    };
  }, []);

  // Load customer-owned cart and favorites after login or session restoration.
  useEffect(() => {
    if (!sessionChecked || !customerEmail) return;
    let active = true;

    Promise.all([
      customerApiRequest<CartApiResponse>("/api/cart"),
      customerApiRequest<FavoriteApiItem[]>("/api/favorites"),
    ])
      .then(([cartData, favoriteData]) => {
        if (!active) return;
        setCart(mapCartItems(cartData));
        setFavorites(new Set(favoriteData.map((favorite) => favorite.productId)));
        setCartError("");
        setFavoritesError("");
      })
      .catch((error) => {
        if (!active) return;
        const message =
          error instanceof Error ? error.message : "Unable to load saved items.";
        setCartError(message);
        setFavoritesError(message);
      });

    return () => {
      active = false;
    };
  }, [sessionChecked, customerEmail]);

  const categories: Category[] = [
    "All Products",
    "Dairy & Cheese",
    "Traditional Pantry",
    "Herbs & Spices",
  ];

  const filteredProducts = products.filter((product) => {
    const matchesCategory =
      selectedCategory === "All Products" ||
      product.category === selectedCategory;

    const matchesSearch = product.name
      .toLowerCase()
      .includes(searchTerm.trim().toLowerCase());

    const matchesFavorites = !favoritesOnly || favorites.has(product.id);

    return matchesCategory && matchesSearch && matchesFavorites;
  });

  const cartCount = cart.reduce((total, item) => total + item.quantity, 0);

  const cartTotal = cart.reduce(
    (total, item) => total + item.product.price * item.quantity,
    0,
  );
  const cartHasUnavailableItem = cart.some(
    (item) => item.quantity > item.product.stockQuantity,
  );

  async function addToCart(product: Product) {
    if (cartBusy) return;

    if (customerEmail) {
      setCartBusy(true);
      setCartError("");
      try {
        const response = await customerApiRequest<CartApiResponse>(
          "/api/cart/items",
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ productId: product.id, quantity: 1 }),
          },
        );
        setCart(mapCartItems(response));
      } catch (error) {
        setCartError(
          error instanceof Error ? error.message : "Unable to add this product.",
        );
      } finally {
        setCartBusy(false);
      }
      return;
    }

    setCart((currentCart) => {
      const existingItem = currentCart.find(
        (item) => item.product.id === product.id,
      );

      if (existingItem) {
        if (existingItem.quantity >= product.stockQuantity) {
          return currentCart;
        }

        return currentCart.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item,
        );
      }

      if (product.stockQuantity < 1) {
        return currentCart;
      }

      return [...currentCart, { product, quantity: 1 }];
    });
  }

  async function changeQuantity(productId: number, change: number) {
    if (cartBusy) return;

    if (customerEmail) {
      const existingItem = cart.find((item) => item.product.id === productId);
      if (!existingItem) return;
      const requestedQuantity = existingItem.quantity + change;
      const quantity = change < 0 && requestedQuantity > existingItem.product.stockQuantity
        ? existingItem.product.stockQuantity
        : requestedQuantity;
      setCartBusy(true);
      setCartError("");
      try {
        const response = quantity <= 0
          ? await customerApiRequest<CartApiResponse>(
              `/api/cart/items/${productId}`,
              { method: "DELETE" },
            )
          : await customerApiRequest<CartApiResponse>(
              `/api/cart/items/${productId}`,
              {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ quantity }),
              },
            );
        setCart(mapCartItems(response));
      } catch (error) {
        setCartError(
          error instanceof Error ? error.message : "Unable to update the cart.",
        );
      } finally {
        setCartBusy(false);
      }
      return;
    }

    setCart((currentCart) =>
      currentCart
        .map((item) => {
          if (item.product.id !== productId) {
            return item;
          }

          const newQuantity = item.quantity + change;

          if (newQuantity > item.product.stockQuantity) {
            return item;
          }

          return { ...item, quantity: newQuantity };
        })
        .filter((item) => item.quantity > 0),
    );
  }

  async function clearCart() {
    if (cartBusy) return;
    if (!customerEmail) {
      setCart([]);
      setCartError("");
      return;
    }

    setCartBusy(true);
    setCartError("");
    try {
      const response = await customerApiRequest<CartApiResponse>("/api/cart", {
        method: "DELETE",
      });
      setCart(mapCartItems(response));
    } catch (error) {
      setCartError(
        error instanceof Error ? error.message : "Unable to clear the cart.",
      );
    } finally {
      setCartBusy(false);
    }
  }

  async function toggleFavorite(product: Product) {
    if (!customerEmail) {
      if (!loggedInUser) setAuthModalOpen(true);
      setFavoritesError("Log in with a customer account to save favorites.");
      return;
    }
    if (favoritesBusy) return;

    setFavoritesBusy(true);
    setFavoritesError("");
    try {
      if (favorites.has(product.id)) {
        await customerApiRequest<void>(`/api/favorites/${product.id}`, {
          method: "DELETE",
        });
        setFavorites((current) => {
          const next = new Set(current);
          next.delete(product.id);
          return next;
        });
      } else {
        await customerApiRequest<FavoriteApiItem>("/api/favorites", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ productId: product.id }),
        });
        setFavorites((current) => new Set(current).add(product.id));
      }
    } catch (error) {
      setFavoritesError(
        error instanceof Error ? error.message : "Unable to update favorites.",
      );
    } finally {
      setFavoritesBusy(false);
    }
  }

  async function openOrderHistory() {
    if (!customerEmail) return;
    setCartOpen(false);
    setCheckoutOpen(false);
    setVendorOrdersOpen(false);
    setVendorWorkspaceOpen(false);
    setAdminManagementOpen(false);
    setOrderHistoryOpen(true);
    scrollToNavigationSection("my-orders-section");
    setSelectedOrder(null);
    setOrdersLoading(true);
    setOrdersError("");
    try {
      const orderData = await customerApiRequest<OrderSummary[]>(
        "/api/orders/my-orders",
      );
      setOrders(orderData);
    } catch (error) {
      setOrdersError(
        error instanceof Error ? error.message : "Unable to load your orders.",
      );
    } finally {
      setOrdersLoading(false);
    }
  }

  async function showOrderDetails(orderId: number) {
    setOrderDetailLoading(true);
    setOrdersError("");
    try {
      const orderDetail = await customerApiRequest<OrderDetail>(
        `/api/orders/my-orders/${orderId}`,
      );
      setSelectedOrder(orderDetail);
    } catch (error) {
      setOrdersError(
        error instanceof Error ? error.message : "Unable to load order details.",
      );
    } finally {
      setOrderDetailLoading(false);
    }
  }

  async function payForOrder(orderId: number) {
    setOrderDetailLoading(true);
    setOrdersError("");
    let detail: OrderDetail;
    try {
      detail = await customerApiRequest<OrderDetail>(
        `/api/orders/my-orders/${orderId}/payment`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ paymentMethod }),
        },
      );
    } catch (error) {
      setOrdersError(error instanceof Error ? error.message : "Unable to capture payment.");
      setOrderDetailLoading(false);
      return;
    }

    setSelectedOrder(detail);
    setOrders((current) => current.map((order) => order.orderId === orderId
      ? { ...order, status: detail.status, invoiceNumber: detail.invoice?.invoiceNumber ?? null }
      : order));
    try {
      const summaries = await customerApiRequest<OrderSummary[]>("/api/orders/my-orders");
      setOrders(summaries);
    } catch {
      setOrdersError("Payment was captured, but the order list could not be refreshed.");
    } finally {
      setOrderDetailLoading(false);
    }
  }

  async function cancelCustomerOrder(orderId: number) {
    setOrderDetailLoading(true);
    setOrdersError("");
    let detail: OrderDetail;
    try {
      detail = await customerApiRequest<OrderDetail>(
        `/api/orders/my-orders/${orderId}/cancel`,
        { method: "POST" },
      );
    } catch (error) {
      setOrdersError(error instanceof Error ? error.message : "Unable to cancel this order.");
      setOrderDetailLoading(false);
      return;
    }

    setSelectedOrder(detail);
    setOrders((current) => current.map((order) => order.orderId === orderId
      ? { ...order, status: detail.status, invoiceNumber: detail.invoice?.invoiceNumber ?? null }
      : order));
    try {
      const summaries = await customerApiRequest<OrderSummary[]>("/api/orders/my-orders");
      setOrders(summaries);
    } catch {
      setOrdersError("The order was cancelled, but the order list could not be refreshed.");
    } finally {
      setOrderDetailLoading(false);
    }
  }

  async function refreshVendorOrders() {
    setVendorOrdersLoading(true);
    setVendorOrdersError("");
    try {
      const data = await customerApiRequest<OrderDetail[]>("/api/vendor/orders");
      setVendorOrders(data);
    } catch (error) {
      setVendorOrdersError(error instanceof Error ? error.message : "Unable to load vendor orders.");
    } finally {
      setVendorOrdersLoading(false);
    }
  }

  async function openVendorOrders() {
    setCartOpen(false);
    setCheckoutOpen(false);
    setOrderHistoryOpen(false);
    setVendorWorkspaceOpen(false);
    setAdminManagementOpen(false);
    setVendorOrdersOpen(true);
    scrollToNavigationSection("vendor-orders-section");
    await refreshVendorOrders();
  }

  async function updateVendorOrder(orderId: number, action: string, body?: object) {
    if (vendorActionBusy) return;
    setVendorActionBusy(true);
    setVendorOrdersError("");
    let updatedOrder: OrderDetail;
    try {
      updatedOrder = await customerApiRequest<OrderDetail>(`/api/vendor/orders/${orderId}/${action}`, {
        method: "PUT",
        headers: body ? { "Content-Type": "application/json" } : undefined,
        body: body ? JSON.stringify(body) : undefined,
      });
    } catch (error) {
      setVendorOrdersError(error instanceof Error ? error.message : "Unable to update this order.");
      setVendorActionBusy(false);
      return;
    }

    setVendorOrders((current) => current.map((order) =>
      order.orderId === orderId ? updatedOrder : order));
    await refreshVendorOrders();
    setVendorActionBusy(false);
  }

  async function rejectVendorOrder(orderId: number) {
    const reason = window.prompt("Enter a reason for rejecting this order:")?.trim();
    if (!reason) return;
    if (reason.length > 500) {
      setVendorOrdersError("The rejection reason must be 500 characters or fewer.");
      return;
    }
    await updateVendorOrder(orderId, "reject", { reason });
  }

  async function handleCheckout(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!customerEmail) {
      setOrderError("Please log in with a customer account before placing your order.");
      return;
    }

    if (new Set(cart.map((item) => item.product.vendorId)).size > 1) {
      setOrderError("Please place separate orders for products from different vendors.");
      return;
    }

    setOrderLoading(true);
    setOrderError("");

    try {
      const orderRequest = {
        deliveryAddress: {
          addressLabel: "Delivery Address",
          addressLine1: customer.address,
          addressLine2: "",
          city: customer.city,
          emirate: customer.emirate,
          postalCode: customer.postalCode,
          country: "United Arab Emirates",
          addressType: "HOME",
        },
        payment: { paymentMethod },
      };

      const orderData = await customerApiRequest<OrderDetail>("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(orderRequest),
      });
      setCart([]);
      setCreatedOrder(orderData);
      setOrderConfirmed(true);
    } catch (error) {
      console.error("Checkout error:", error);

      setOrderError(
        error instanceof Error
          ? error.message
          : "Something went wrong while placing your order.",
      );
    } finally {
      setOrderLoading(false);
    }
  }
  async function handleLogout() {
    try {
      const csrfResponse = await fetch("http://localhost:8080/api/auth/csrf", {
        credentials: "include",
      });

      const csrfData = await csrfResponse.json();

      const csrfToken = document.cookie
  .split("; ")
  .find((cookie) => cookie.startsWith("XSRF-TOKEN="))
  ?.substring("XSRF-TOKEN=".length);

      await fetch("http://localhost:8080/api/auth/logout", {
        method: "POST",
        credentials: "include",
        headers: {
          [csrfData.headerName]: decodeURIComponent(
            csrfToken || csrfData.token,
          ),
        },
      });

    } catch (error) {
      console.error("Logout error:", error);
    } finally {
      setLoggedInUser(null);
      setCart([]);
      setFavorites(new Set());
      setFavoritesOnly(false);
      setCartError("");
      setFavoritesError("");
      setOrders([]);
      setSelectedOrder(null);
      setVendorOrders([]);
      setVendorOrdersOpen(false);
      setVendorWorkspaceOpen(false);
      setAdminManagementOpen(false);
      setOrderHistoryOpen(false);
      setOrdersError("");
      setVendorOrdersError("");
      setCreatedOrder(null);
      setOrderConfirmed(false);
      setCheckoutOpen(false);
      setCartOpen(false);
      setCustomer({ address: "", city: "", emirate: "", postalCode: "" });
    }
  }
  return (
    <div className={`storefront ${darkMode ? "dark-mode" : ""}`}>
      <div className="announcement-bar">
        A taste of tradition, made to bring people together.
      </div>

      <Navbar expand="lg" className="store-navbar">
        <Container>
          <Navbar.Brand href="#home" className="brand">
            <img src={logo} alt="Um Hamza Foodstuff Factory" />
          </Navbar.Brand>

          <Navbar.Toggle aria-controls="store-navigation" />

          <Navbar.Collapse id="store-navigation">
            <Nav className="mx-auto navigation-links">
              <Nav.Link href="#home">Home</Nav.Link>
              <Nav.Link href="#products">Our Products</Nav.Link>
              <Nav.Link href="#about">Our Story</Nav.Link>
            </Nav>

            <Button
              className="theme-toggle-button"
              variant="outline-secondary"
              onClick={() => setDarkMode(!darkMode)}
              aria-label={
                darkMode ? "Switch to light mode" : "Switch to dark mode"
              }
            >
              {darkMode ? "☀" : "☾"}
            </Button>

            <div className="navbar-actions">
              {!loggedInUser ? (
                <Button
                  className="login-button"
                  onClick={() => setAuthModalOpen(true)}
                >
                  Login / Register
                </Button>
              ) : (
                <Button className="login-button" onClick={handleLogout}>
                  Logout
                </Button>
              )}

              {customerEmail && (
                <Button
                  className="login-button"
                  variant="outline-success"
                  onClick={openOrderHistory}
                >
                  My Orders
                </Button>
              )}

              {canManageVendorOrders && (
                <Button
                  className="login-button"
                  variant="outline-success"
                  onClick={openVendorOrders}
                >
                  Vendor Orders
                </Button>
              )}

              {isVendor && (
                <Button
                  className="login-button"
                  variant="outline-success"
                  onClick={() => {
                    setVendorWorkspaceOpen(true);
                    setAdminManagementOpen(false);
                    setVendorOrdersOpen(false);
                    setOrderHistoryOpen(false);
                    setCartOpen(false);
                    scrollToNavigationSection("vendor-workspace-section");
                  }}
                >
                  Vendor Workspace
                </Button>
              )}

              {isAdmin && (
                <Button
                  className="login-button"
                  variant="outline-success"
                  onClick={() => {
                    setAdminManagementOpen(true);
                    setVendorWorkspaceOpen(false);
                    setVendorOrdersOpen(false);
                    setOrderHistoryOpen(false);
                    setCartOpen(false);
                    scrollToNavigationSection("admin-management-section");
                  }}
                >
                  Admin Management
                </Button>
              )}

              <Button
                className="cart-button"
                onClick={() => {
                  setCartOpen(!cartOpen);
                  setCheckoutOpen(false);
                }}
                aria-expanded={cartOpen}
              >
                Cart{" "}
                <Badge bg="light" text="dark">
                  {cartCount}
                </Badge>
              </Button>
            </div>
          </Navbar.Collapse>
        </Container>
      </Navbar>
      {authModalOpen && (
        <div
          className="auth-modal-backdrop"
          onClick={() => setAuthModalOpen(false)}
        >
          <div
            className="auth-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <button
              className="auth-modal-close"
              onClick={() => setAuthModalOpen(false)}
              aria-label="Close login modal"
            >
              ×
            </button>

            <Auth
              onLogin={(user) => {
                setCart([]);
                setFavorites(new Set());
                setOrders([]);
                setSelectedOrder(null);
                setVendorOrders([]);
                setVendorOrdersOpen(false);
                setVendorWorkspaceOpen(false);
                setAdminManagementOpen(false);
                setLoggedInUser(user);
                setAuthModalOpen(false);
              }}
            />
          </div>
        </div>
      )}
      {loggedInUser && (
        <Container className="py-3">
          <div className="alert alert-success mb-0 d-flex justify-content-between align-items-center">
            <div>
              Welcome, <strong>{loggedInUser.email}</strong>!
              <br />
              <small>Role: {loggedInUser.roles.join(", ")}</small>
            </div>

            <Button variant="outline-success" onClick={handleLogout}>
              Logout
            </Button>
          </div>
        </Container>
      )}

      <main>
        <section id="home" className="hero-section">
          <Container>
            <Row className="align-items-center g-5">
              <Col lg={6}>
                <span className="eyebrow">ROOTED IN TRADITION</span>

                <h1>
                  Authentic flavours.
                  <br />
                  <span>Made with tradition.</span>
                </h1>

                <p className="hero-description">
                  Discover traditional flavours inspired by the food, culture,
                  and hospitality that bring us together.
                </p>

                <Button className="primary-button" href="#products">
                  Explore Our Products <span aria-hidden="true">→</span>
                </Button>
              </Col>

              <Col lg={6}>
                <div className="hero-image-wrapper">
                  <img
                    src={groupImage}
                    alt="A selection of Um Hamza traditional food products"
                    className="hero-image"
                  />
                  <div className="hero-image-caption">
                    <span>UM HAMZA</span>
                    <span>Tradition in every taste</span>
                  </div>
                </div>
              </Col>
            </Row>
          </Container>
        </section>

        <section id="products" className="products-section">
          <Container>
            <div className="section-heading text-center">
              <span className="eyebrow">DISCOVER OUR SELECTION</span>
              <h2>
                Good food, <span>good memories.</span>
              </h2>
              <p>
                Explore a selection of traditional favourites from Um Hamza.
              </p>
            </div>

            <div className="product-controls">
              <div className="category-filters" aria-label="Product categories">
                {categories.map((category) => (
                  <Button
                    key={category}
                    className={
                      selectedCategory === category
                        ? "category-button active"
                        : "category-button"
                    }
                    onClick={() => setSelectedCategory(category)}
                    aria-pressed={selectedCategory === category}
                  >
                    {category}
                  </Button>
                ))}
                <Button
                  className={favoritesOnly ? "category-button active" : "category-button"}
                  onClick={() => {
                    if (!customerEmail) {
                      if (!loggedInUser) setAuthModalOpen(true);
                      setFavoritesError(
                        "Log in with a customer account to view favorites.",
                      );
                      return;
                    }
                    setFavoritesOnly((current) => !current);
                    setFavoritesError("");
                  }}
                  aria-pressed={favoritesOnly}
                >
                  Favorites ({favorites.size})
                </Button>
              </div>

              <Form.Control
                type="search"
                placeholder="Search products..."
                aria-label="Search products"
                className="product-search"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
            </div>
            {favoritesError && (
              <p className="text-center text-danger" role="alert">
                {favoritesError}
              </p>
            )}
            {cartError && (
              <p className="text-center text-danger" role="alert">
                {cartError}
              </p>
            )}
            {productsLoading && (
              <p className="text-center">Loading products...</p>
            )}

            {productsError && (
              <p className="text-center text-danger">{productsError}</p>
            )}

            <Row className="g-4">
              {filteredProducts.map((product) => (
                <Col key={product.id} xs={12} sm={6} lg={4} xl={3}>
                  <Card className="product-card h-100">
                    <div className="product-image-wrapper">
                      <ProductImage
                        image={product.image}
                        alt={product.name}
                        imageClassName="product-image"
                        placeholderClassName="product-image-placeholder"
                      />
                      <Button
                        className={
                          favorites.has(product.id)
                            ? "favorite-toggle active"
                            : "favorite-toggle"
                        }
                        onClick={() => toggleFavorite(product)}
                        aria-label={
                          favorites.has(product.id)
                            ? `Remove ${product.name} from favorites`
                            : `Add ${product.name} to favorites`
                        }
                        aria-pressed={favorites.has(product.id)}
                        disabled={favoritesBusy}
                      >
                        {favorites.has(product.id) ? "♥" : "♡"}
                      </Button>
                    </div>

                    <Card.Body className="d-flex flex-column">
                      <span className="product-category">
                        {product.category}
                      </span>

                      <Card.Title>{product.name}</Card.Title>
                      <Card.Text>{product.description}</Card.Text>

                      <p className="product-stock">
                        {product.stockQuantity <= 5
                          ? `Only ${product.stockQuantity} left in stock`
                          : "In Stock"}
                      </p>

                      <div className="product-card-footer mt-auto">
                        <span className="product-price">
                          {formatPrice(product.price)}
                        </span>

                        {cart.find((item) => item.product.id === product.id) ? (
                          <div className="product-actions">
                            <div className="product-quantity-controls">
                              <Button
                                onClick={() => changeQuantity(product.id, -1)}
                                disabled={cartBusy}
                                aria-label={`Decrease ${product.name} quantity`}
                              >
                                −
                              </Button>

                              <span>
                                {
                                  cart.find(
                                    (item) => item.product.id === product.id,
                                  )?.quantity
                                }{" "}
                                in cart
                              </span>

                              <Button
                                onClick={() => changeQuantity(product.id, 1)}
                                disabled={cartBusy}
                                aria-label={`Increase ${product.name} quantity`}
                              >
                                +
                              </Button>
                            </div>

                            <Button
                              className="add-button"
                              onClick={() => {
                                setCartOpen(true);
                                setCheckoutOpen(false);
                              }}
                            >
                              Go to Cart
                            </Button>
                          </div>
                        ) : (
                          <Button
                            className="add-button"
                            onClick={() => addToCart(product)}
                            disabled={cartBusy}
                            aria-label={`Add ${product.name} to cart`}
                          >
                            + Add to Cart
                          </Button>
                        )}
                      </div>
                    </Card.Body>
                  </Card>
                </Col>
              ))}
            </Row>

            {filteredProducts.length === 0 && (
              <div className="empty-results">
                <h3>{favoritesOnly ? "No favorite products yet" : "No products found"}</h3>
                <p>
                  {favoritesOnly
                    ? "Save a product with the heart button to see it here."
                    : "Try another search term or select a different category."}
                </p>
                <Button
                  className="primary-button"
                  onClick={() => {
                    setSearchTerm("");
                    setSelectedCategory("All Products");
                    setFavoritesOnly(false);
                  }}
                >
                  Show All Products
                </Button>
              </div>
            )}
          </Container>
        </section>

        {orderHistoryOpen && customerEmail && (
          <section
            id="my-orders-section"
            className="cart-section navigation-scroll-target"
            aria-label="Order history"
          >
            <Container>
              <div className="cart-panel">
                <div className="cart-heading">
                  <div>
                    <span className="eyebrow">YOUR ACCOUNT</span>
                    <h2>My Orders</h2>
                  </div>
                  <Button
                    variant="outline-secondary"
                    onClick={() => {
                      setOrderHistoryOpen(false);
                      setSelectedOrder(null);
                    }}
                  >
                    Close
                  </Button>
                </div>

                {ordersLoading && <p>Loading your orders...</p>}
                {ordersError && (
                  <div className="alert alert-danger" role="alert">
                    {ordersError}
                  </div>
                )}
                {!ordersLoading && !ordersError && orders.length === 0 && (
                  <p>You have not placed any orders yet.</p>
                )}

                {orders.map((order) => (
                  <div className="checkout-summary-item" key={order.orderId}>
                    <span>
                      <strong>{order.orderNumber}</strong>
                      <br />
                      <small>
                        {new Date(order.orderDate).toLocaleString()} · {displayOrderStatus(order.status)}
                        {order.invoiceNumber ? ` · ${order.invoiceNumber}` : ""}
                      </small>
                    </span>
                    <span className="d-flex align-items-center gap-3">
                      <strong>{formatPrice(order.totalAmount)}</strong>
                      <Button
                        variant="outline-success"
                        size="sm"
                        onClick={() => showOrderDetails(order.orderId)}
                        disabled={orderDetailLoading}
                      >
                        View Details
                      </Button>
                    </span>
                  </div>
                ))}

                {orderDetailLoading && <p>Loading order details...</p>}
                {selectedOrder && (
                  <div className="mt-4">
                    <h3>Order {selectedOrder.orderNumber}</h3>
                    <p>
                      {selectedOrder.deliveryAddress.addressLine1}
                      {selectedOrder.deliveryAddress.addressLine2
                        ? `, ${selectedOrder.deliveryAddress.addressLine2}`
                        : ""}
                      , {selectedOrder.deliveryAddress.city}, {selectedOrder.deliveryAddress.emirate}{" "}
                      {selectedOrder.deliveryAddress.postalCode ?? ""}, {selectedOrder.deliveryAddress.country}
                    </p>
                    {selectedOrder.items.map((item, index) => (
                      <div className="checkout-summary-item" key={`${item.productId}-${index}`}>
                        <span>{item.productName} × {item.quantity}</span>
                        <strong>{formatPrice(item.lineTotal)}</strong>
                      </div>
                    ))}
                    <div className="checkout-summary-total">
                      <span>Subtotal</span>
                      <strong>{formatPrice(selectedOrder.subtotal)}</strong>
                    </div>
                    <div className="checkout-summary-item">
                      <span>Tax and shipping</span>
                      <strong>
                        {formatPrice(selectedOrder.taxAmount + selectedOrder.shippingCost)}
                      </strong>
                    </div>
                    <div className="checkout-summary-total">
                      <span>Total</span>
                      <strong>{formatPrice(selectedOrder.totalAmount)}</strong>
                    </div>
                    {selectedOrder.invoice && (
                      <p className="mt-3 mb-0">
                        Invoice {selectedOrder.invoice.invoiceNumber} · Issued {selectedOrder.invoice.issueDate} · {selectedOrder.invoice.status}
                      </p>
                    )}
                    {selectedOrder.payment ? (
                      <p className="mt-3 mb-0">
                        Payment: {selectedOrder.payment.paymentMethod} · {selectedOrder.payment.status}
                        {selectedOrder.payment.status === "CAPTURED"
                          ? ` · ${formatPrice(selectedOrder.payment.capturedAmount)}`
                          : ""}
                      </p>
                    ) : (
                      <p className="mt-3 mb-0">Payment has not been captured.</p>
                    )}
                    {selectedOrder.delivery && (
                      <p className="mt-2 mb-0">
                        Delivery: {displayOrderStatus(selectedOrder.delivery.status)}
                        {selectedOrder.delivery.actualDeliveryDate
                          ? ` · delivered ${selectedOrder.delivery.actualDeliveryDate}`
                          : ""}
                      </p>
                    )}
                    {selectedOrder.statusHistory.length > 0 && (
                      <div className="mt-3">
                        <strong>Order updates</strong>
                        {selectedOrder.statusHistory.map((entry, index) => (
                          <p className="mb-1" key={`${entry.changedAt}-${index}`}>
                            <small>
                              {new Date(entry.changedAt).toLocaleString()} · {displayOrderStatus(entry.status)}
                              {entry.comment ? ` · ${entry.comment}` : ""}
                            </small>
                          </p>
                        ))}
                      </div>
                    )}
                    {selectedOrder.status === "APPROVED_AWAITING_PAYMENT" && (
                      <div className="d-flex align-items-center gap-2 mt-3">
                        <Form.Select
                          aria-label="Payment method"
                          value={paymentMethod}
                          onChange={(event) => setPaymentMethod(event.target.value)}
                          style={{ maxWidth: 220 }}
                        >
                          <option value="CREDIT">Credit Card (simulated)</option>
                          <option value="DEBIT">Debit Card (simulated)</option>
                        </Form.Select>
                        <Button
                          variant="success"
                          disabled={orderDetailLoading}
                          onClick={() => void payForOrder(selectedOrder.orderId)}
                        >
                          Pay now
                        </Button>
                      </div>
                    )}
                    {["PENDING_VENDOR_APPROVAL", "APPROVED_AWAITING_PAYMENT", "PROCESSING", "READY_FOR_DELIVERY"]
                      .includes(selectedOrder.status) && (
                      <Button
                        className="mt-3"
                        variant="outline-danger"
                        disabled={orderDetailLoading}
                        onClick={() => void cancelCustomerOrder(selectedOrder.orderId)}
                      >
                        Cancel order
                      </Button>
                    )}
                  </div>
                )}
              </div>
            </Container>
          </section>
        )}

        {vendorOrdersOpen && canManageVendorOrders && (
          <section id="vendor-orders-section" className="cart-section navigation-scroll-target" aria-label="Vendor orders">
            <Container>
              <div className="cart-panel">
                <div className="cart-heading">
                  <div>
                    <span className="eyebrow">ORDER MANAGEMENT</span>
                    <h2>Vendor Orders</h2>
                  </div>
                  <Button variant="outline-secondary" onClick={() => setVendorOrdersOpen(false)}>
                    Close
                  </Button>
                </div>
                {vendorOrdersError && (
                  <div className="alert alert-danger" role="alert">{vendorOrdersError}</div>
                )}
                {vendorOrdersLoading && <p>Loading orders...</p>}
                {!vendorOrdersLoading && !vendorOrdersError && vendorOrders.length === 0 && (
                  <p>No orders are available for this vendor account.</p>
                )}
                {vendorOrders.map((order) => (
                  <div className="border rounded p-3 mb-3" key={order.orderId}>
                    <div className="d-flex justify-content-between align-items-start gap-3">
                      <div>
                        <strong>{order.orderNumber}</strong>
                        <div>{displayOrderStatus(order.status)} · {formatPrice(order.totalAmount)}</div>
                        <small>{new Date(order.orderDate).toLocaleString()}</small>
                      </div>
                      {order.status === "PENDING_VENDOR_APPROVAL" && (
                        <div className="d-flex gap-2">
                          <Button
                            size="sm"
                            variant="success"
                            disabled={vendorActionBusy}
                            onClick={() => void updateVendorOrder(order.orderId, "approve")}
                          >
                            Approve
                          </Button>
                          <Button
                            size="sm"
                            variant="outline-danger"
                            disabled={vendorActionBusy}
                            onClick={() => void rejectVendorOrder(order.orderId)}
                          >
                            Reject
                          </Button>
                        </div>
                      )}
                      {order.status === "PROCESSING" && (
                        <Button size="sm" disabled={vendorActionBusy}
                          onClick={() => void updateVendorOrder(order.orderId, "delivery-status", { status: "READY_FOR_DELIVERY" })}>
                          Mark ready
                        </Button>
                      )}
                      {order.status === "READY_FOR_DELIVERY" && (
                        <Button size="sm" disabled={vendorActionBusy}
                          onClick={() => void updateVendorOrder(order.orderId, "delivery-status", { status: "OUT_FOR_DELIVERY" })}>
                          Mark shipped
                        </Button>
                      )}
                      {order.status === "OUT_FOR_DELIVERY" && (
                        <Button size="sm" disabled={vendorActionBusy}
                          onClick={() => void updateVendorOrder(order.orderId, "delivery-status", { status: "DELIVERED" })}>
                          Mark delivered
                        </Button>
                      )}
                    </div>
                    <ul className="mt-3 mb-0">
                      {order.items.map((item, index) => (
                        <li key={`${order.orderId}-${item.productId}-${index}`}>
                          {item.productName} × {item.quantity}
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            </Container>
          </section>
        )}

        {vendorWorkspaceOpen && isVendor && (
          <CatalogManagementPanel
            sectionId="vendor-workspace-section"
            isAdmin={false}
            vendorId={loggedInUser?.vendorId}
            apiRequest={customerApiRequest}
            onProductsChanged={() => setCatalogReloadKey((key) => key + 1)}
            onClose={() => setVendorWorkspaceOpen(false)}
          />
        )}

        {adminManagementOpen && isAdmin && (
          <CatalogManagementPanel
            sectionId="admin-management-section"
            isAdmin
            apiRequest={customerApiRequest}
            onProductsChanged={() => setCatalogReloadKey((key) => key + 1)}
            onClose={() => setAdminManagementOpen(false)}
          />
        )}

        {cartOpen && (
          <section
            id="shopping-cart"
            className="cart-section"
            aria-label="Shopping cart"
          >
            <Container>
              <div className="cart-panel">
                {cartError && (
                  <div className="alert alert-danger" role="alert">
                    {cartError}
                  </div>
                )}
                {orderConfirmed ? (
                  <div className="order-confirmation">
                    <div className="confirmation-icon">✓</div>

                    <span className="eyebrow">ORDER CONFIRMED</span>

                    <h2>Thank you for your order!</h2>

                    <p>
                      Your simulated payment was captured with the order. The
                      vendor will review it before fulfillment.
                    </p>

                    <div className="confirmation-notice">
                      <strong>Order Number:</strong>{" "}
                      {createdOrder?.orderNumber || createdOrder?.orderId}
                      <br />
                      <strong>Order Status:</strong>{" "}
                      {displayOrderStatus(createdOrder?.status || "PENDING_VENDOR_APPROVAL")}
                      {createdOrder?.invoice?.invoiceNumber && (
                        <>
                          <br />
                          <strong>Invoice Number:</strong> {createdOrder.invoice.invoiceNumber}
                        </>
                      )}
                      {createdOrder?.payment && (
                        <>
                          <br />
                          <strong>Payment:</strong> {createdOrder.payment.paymentMethod} · {createdOrder.payment.status}
                        </>
                      )}
                    </div>

                    <h3>Order Summary</h3>

                    {(createdOrder?.items ?? []).map((item, index) => (
                      <div
                        className="checkout-summary-item"
                        key={`${item.productId}-${index}`}
                      >
                        <span>
                          {item.productName} × {item.quantity}
                        </span>
                        <strong>
                          {formatPrice(item.lineTotal)}
                        </strong>
                      </div>
                    ))}

                    <div className="checkout-summary-total">
                      <span>Order Total</span>
                      <strong>{formatPrice(createdOrder?.totalAmount ?? cartTotal)}</strong>
                    </div>

                    <Button
                      className="primary-button"
                      onClick={() => {
                        setOrderConfirmed(false);
                        setCheckoutOpen(false);
                        setCartOpen(false);
                        setCart([]);
                        setCustomer({
                          address: "",
                          city: "",
                          emirate: "",
                          postalCode: "",
                        });
                      }}
                    >
                      Return to Storefront
                    </Button>
                    {customerEmail && (
                      <Button
                        className="primary-button"
                        variant="outline-success"
                        onClick={() => {
                          setOrderConfirmed(false);
                          setCartOpen(false);
                          setCart([]);
                          void openOrderHistory();
                        }}
                      >
                        View My Orders
                      </Button>
                    )}
                  </div>
                ) : checkoutOpen ? (
                  <div className="checkout-form">
                    <div className="cart-heading">
                      <div>
                        <span className="eyebrow">YOUR ORDER</span>
                        <h2>Checkout</h2>
                      </div>

                      <Button
                        variant="outline-secondary"
                        onClick={() => setCartOpen(false)}
                      >
                        Close
                      </Button>
                    </div>

                    <p>
                      Enter your delivery details and choose a simulated payment
                      method. Payment, order, and invoice are saved together.
                    </p>

                    {!customerEmail && (
                      <div className="alert alert-info" role="status">
                        Log in with a customer account before placing an order.
                        <Button
                          variant="link"
                          onClick={() => setAuthModalOpen(true)}
                        >
                          Login / Register
                        </Button>
                      </div>
                    )}

                    <div className="checkout-order-summary">
                      <h3>Order Summary</h3>

                      {cart.map((item) => (
                        <div
                          className="checkout-summary-item"
                          key={item.product.id}
                        >
                          <span>
                            {item.product.name} × {item.quantity}
                          </span>

                          <strong>
                            {formatPrice(item.product.price * item.quantity)}
                          </strong>
                        </div>
                      ))}

                      <div className="checkout-summary-total">
                        <span>Subtotal</span>
                        <strong>{formatPrice(cartTotal)}</strong>
                      </div>
                    </div>

                    <Form onSubmit={handleCheckout}>
                      <Form.Group className="mb-3">
                        <Form.Label>Delivery Address</Form.Label>
                        <Form.Control
                          as="textarea"
                          rows={3}
                          placeholder="Enter your delivery address"
                          value={customer.address}
                          onChange={(event) =>
                            setCustomer({
                              ...customer,
                              address: event.target.value,
                            })
                          }
                          required
                        />
                      </Form.Group>

                      <Row>
                        <Col md={6}>
                          <Form.Group className="mb-3">
                            <Form.Label>City</Form.Label>
                            <Form.Control
                              value={customer.city}
                              onChange={(event) =>
                                setCustomer({ ...customer, city: event.target.value })
                              }
                              required
                            />
                          </Form.Group>
                        </Col>
                        <Col md={6}>
                          <Form.Group className="mb-3">
                            <Form.Label>Emirate</Form.Label>
                            <Form.Control
                              value={customer.emirate}
                              onChange={(event) =>
                                setCustomer({ ...customer, emirate: event.target.value })
                              }
                              required
                            />
                          </Form.Group>
                        </Col>
                      </Row>
                      <Form.Group className="mb-3">
                        <Form.Label>Postal Code (optional)</Form.Label>
                        <Form.Control
                          value={customer.postalCode}
                          onChange={(event) =>
                            setCustomer({ ...customer, postalCode: event.target.value })
                          }
                        />
                      </Form.Group>

                      <Form.Group className="mb-3">
                        <Form.Label>Payment Method</Form.Label>
                        <Form.Select
                          value={paymentMethod}
                          onChange={(event) => setPaymentMethod(event.target.value)}
                          required
                        >
                          <option value="CREDIT">Credit Card (simulated)</option>
                          <option value="DEBIT">Debit Card (simulated)</option>
                        </Form.Select>
                      </Form.Group>

                      <div className="alert alert-info mt-3 mb-4">
                        This student project simulates payment locally. Your
                        payment, order, inventory reservation, and invoice are
                        processed as one transaction.
                      </div>

                      {/* Order Total */}
                      <div className="cart-total">
                        <span>Order Total</span>
                        <strong>{formatPrice(cartTotal)}</strong>
                      </div>

                      {/* Error Message */}
                      {orderError && (
                        <div className="alert alert-danger mt-3">
                          {orderError}
                        </div>
                      )}

                      {/* Submit */}
                      <Button
                        type="submit"
                        className="primary-button"
                        disabled={orderLoading || !customerEmail}
                      >
                        {orderLoading ? "Processing Purchase..." : "Place Order"}
                      </Button>

                      <Button
                        variant="link"
                        onClick={() => setCheckoutOpen(false)}
                      >
                        Back to Cart
                      </Button>
                    </Form>
                  </div>
                ) : (
                  <>
                    <div className="cart-heading">
                      <div>
                        <span className="eyebrow">YOUR SELECTION</span>
                        <h2>Shopping Cart</h2>
                      </div>

                      <Button
                        variant="outline-secondary"
                        onClick={() => setCartOpen(false)}
                      >
                        Close
                      </Button>
                    </div>

                    {cart.length === 0 ? (
                      <p className="empty-cart">
                        Your cart is empty. Explore our products to get started.
                      </p>
                    ) : (
                      <>
                        {cart.map((item) => (
                          <div className="cart-item" key={item.product.id}>
                            <ProductImage
                              image={item.product.image}
                              alt={item.product.name}
                              imageClassName="cart-item-image"
                              placeholderClassName="cart-item-image-placeholder"
                            />

                            <div className="cart-item-details">
                              <strong>{item.product.name}</strong>
                              <span>
                                {formatPrice(item.product.price)} each
                              </span>
                            </div>

                            <div className="quantity-controls">
                              <Button
                                variant="outline-secondary"
                                onClick={() =>
                                  changeQuantity(item.product.id, -1)
                                }
                                disabled={cartBusy}
                                aria-label={`Decrease ${item.product.name} quantity`}
                              >
                                −
                              </Button>

                              <span>{item.quantity}</span>

                              <Button
                                variant="outline-secondary"
                                onClick={() =>
                                  changeQuantity(item.product.id, 1)
                                }
                                disabled={
                                  cartBusy ||
                                  item.quantity >= item.product.stockQuantity
                                }
                                aria-label={`Increase ${item.product.name} quantity`}
                              >
                                +
                              </Button>
                            </div>

                            <Button
                              variant="link"
                              className="remove-cart-item-button"
                              onClick={() =>
                                changeQuantity(
                                  item.product.id,
                                  -item.quantity,
                                )
                              }
                              disabled={cartBusy}
                              aria-label={`Remove ${item.product.name} from cart`}
                            >
                              Remove
                            </Button>

                            {item.quantity > item.product.stockQuantity && (
                              <small className="text-danger">
                                This product is unavailable or stock changed. Reduce or remove it to continue.
                              </small>
                            )}

                            <strong className="cart-item-total">
                              {formatPrice(item.product.price * item.quantity)}
                            </strong>
                          </div>
                        ))}

                        <div className="cart-total">
                          <span>Subtotal</span>
                          <strong>{formatPrice(cartTotal)}</strong>
                        </div>

                        <p className="cart-note">
                          Sample prices for the school project. Sign in with a
                          customer account to place an order.
                        </p>

                        <Button
                          className="primary-button"
                          onClick={() => setCheckoutOpen(true)}
                          disabled={cartBusy || cartHasUnavailableItem}
                        >
                          Continue to Checkout
                        </Button>

                        <Button
                          variant="link"
                          className="clear-cart-button"
                          onClick={clearCart}
                          disabled={cartBusy}
                        >
                          Clear Cart
                        </Button>
                      </>
                    )}
                  </>
                )}
              </div>
            </Container>
          </section>
        )}

        <section id="about" className="about-section">
          <Container>
            <span className="eyebrow">OUR PHILOSOPHY</span>
            <h2>Tradition brings us together.</h2>
            <p>
              At Um Hamza, food is more than what we share at the table. It is a
              connection to our roots, our memories, and the people we love.
            </p>
          </Container>
        </section>
      </main>

      <footer className="store-footer">
        <Container>
          <div className="footer-content">
            <span>© {new Date().getFullYear()} Um Hamza Foodstuff Factory</span>
          </div>
        </Container>
      </footer>
    </div>
  );
}

export default App;
