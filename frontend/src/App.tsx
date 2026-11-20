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
import labanaShattaImage from "./assets/products/Labana Shatta.png";
import sumacBaladiImage from "./assets/products/Sumac Baladi.png";
import walnutSaladImage from "./assets/products/WalnutSalad.png";
import groupImage from "./assets/groupummhamza.png";

import "./App.css";
import Auth from "./Auth";

type Category =
  | "All Products"
  | "Dairy & Cheese"
  | "Traditional Pantry"
  | "Herbs & Spices";

type Product = {
  id: number;
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
  packageSize: number;
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

const productImagesBySku: Record<string, string> = {
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
  type CreatedOrder = {
    orderId?: number;
    orderNumber?: string;
    status?: string;
    subtotal?: number;
    totalAmount?: number;
    total?: number;
  };

  const [paymentMethod, setPaymentMethod] = useState("CREDIT");

  const [cardDetails, setCardDetails] = useState({
    cardholderName: "",
    cardNumber: "",
    expiryDate: "",
    cvv: "",
  });

  const [createdOrder, setCreatedOrder] = useState<CreatedOrder | null>(null);
  const [darkMode, setDarkMode] = useState(false);

  const [loggedInUser, setLoggedInUser] = useState<{
    email: string;
    roles: string[];
  } | null>(null);
  const [sessionChecked, setSessionChecked] = useState(false);
  const customerEmail = loggedInUser?.roles.includes("ROLE_CUSTOMER")
    ? loggedInUser.email
    : null;

  const [authModalOpen, setAuthModalOpen] = useState(false);

  const [customer, setCustomer] = useState({
    name: "",
    email: "",
    phone: "",
    address: "",
  });

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

  async function handleCheckout(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!loggedInUser) {
      setOrderError("Please login before placing your order.");
      return;
    }

    setOrderLoading(true);
    setOrderError("");

    try {
      // Get CSRF token
      const csrfResponse = await fetch("http://localhost:8080/api/auth/csrf", {
        credentials: "include",
      });

      if (!csrfResponse.ok) {
        throw new Error("Unable to obtain security token.");
      }

      const csrfData = await csrfResponse.json();

     // Get CSRF token from the cookie
const csrfToken = document.cookie
  .split("; ")
  .find((cookie) => cookie.startsWith("XSRF-TOKEN="))
  ?.substring("XSRF-TOKEN=".length);

      // Prepare order request
      const orderRequest = {
        deliveryAddress: {
          addressLabel: "Delivery Address",
          addressLine1: customer.address,
          addressLine2: "",
          city: "Dubai",
          emirate: "Dubai",
          postalCode: "00000",
          country: "United Arab Emirates",
          addressType: "HOME",
        },
        items: cart.map((item) => ({
          productId: item.product.id,
          quantity: item.quantity,
        })),
        paymentMethod: paymentMethod,
      };

      // Submit order
      const orderResponse = await fetch("http://localhost:8080/api/orders", {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          [csrfData.headerName]: decodeURIComponent(
            csrfToken || csrfData.token,
          ),
        },
        body: JSON.stringify(orderRequest),
      });

      const orderData = await orderResponse.json();

      if (!orderResponse.ok) {
        throw new Error(orderData.message || "Unable to place your order.");
      }

      if (customerEmail) {
        try {
          await customerApiRequest<CartApiResponse>("/api/cart", {
            method: "DELETE",
          });
        } catch {
          setCartError(
            "Your order was placed, but the saved cart could not be cleared.",
          );
        }
      }

      // Save real backend order response
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

                    <h2>Thank you, {customer.name}!</h2>

                    <p>Your order has been successfully placed.</p>

                    <div className="confirmation-notice">
                      <strong>Order Number:</strong>{" "}
                      {createdOrder?.orderNumber || createdOrder?.orderId}
                      <br />
                      <strong>Order Status:</strong>{" "}
                      {createdOrder?.status || "PENDING_VENDOR_APPROVAL"}
                    </div>

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
                      <span>Order Total</span>
                      <strong>{formatPrice(cartTotal)}</strong>
                    </div>

                    <Button
                      className="primary-button"
                      onClick={() => {
                        setOrderConfirmed(false);
                        setCheckoutOpen(false);
                        setCartOpen(false);
                        setCart([]);
                        setCustomer({
                          name: "",
                          email: "",
                          phone: "",
                          address: "",
                        });
                      }}
                    >
                      Return to Storefront
                    </Button>
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
                      Please enter your details to continue with your order.
                    </p>

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
                      {/* Customer Information */}
                      <Form.Group className="mb-3">
                        <Form.Label>Full Name</Form.Label>
                        <Form.Control
                          type="text"
                          placeholder="Enter your full name"
                          value={customer.name}
                          onChange={(event) =>
                            setCustomer({
                              ...customer,
                              name: event.target.value,
                            })
                          }
                          required
                        />
                      </Form.Group>

                      <Form.Group className="mb-3">
                        <Form.Label>Email Address</Form.Label>
                        <Form.Control
                          type="email"
                          placeholder="Enter your email"
                          value={customer.email}
                          onChange={(event) =>
                            setCustomer({
                              ...customer,
                              email: event.target.value,
                            })
                          }
                          required
                        />
                      </Form.Group>

                      <Form.Group className="mb-3">
                        <Form.Label>Phone Number</Form.Label>
                        <Form.Control
                          type="tel"
                          placeholder="Enter your phone number"
                          value={customer.phone}
                          onChange={(event) =>
                            setCustomer({
                              ...customer,
                              phone: event.target.value,
                            })
                          }
                          required
                        />
                      </Form.Group>

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

                      {/* Payment Method */}
                      <div className="payment-section mt-4 mb-4">
                        <h5 className="mb-3">Payment Method</h5>

                        <Form.Check
                          type="radio"
                          name="paymentMethod"
                          id="creditPayment"
                          label="Credit Card"
                          value="CREDIT"
                          checked={paymentMethod === "CREDIT"}
                          onChange={(event) =>
                            setPaymentMethod(event.target.value)
                          }
                          className="mb-2"
                        />

                        <Form.Check
                          type="radio"
                          name="paymentMethod"
                          id="debitPayment"
                          label="Debit Card"
                          value="DEBIT"
                          checked={paymentMethod === "DEBIT"}
                          onChange={(event) =>
                            setPaymentMethod(event.target.value)
                          }
                          className="mb-3"
                        />

                        {/* Simulated Card Details */}
                        <div className="card-payment-details">
                          <Form.Group className="mb-3">
                            <Form.Label>Cardholder Name</Form.Label>
                            <Form.Control
                              type="text"
                              placeholder="Name on card"
                              value={cardDetails.cardholderName}
                              onChange={(event) =>
                                setCardDetails({
                                  ...cardDetails,
                                  cardholderName: event.target.value,
                                })
                              }
                              required
                            />
                          </Form.Group>

                          <Form.Group className="mb-3">
                            <Form.Label>Card Number</Form.Label>
                            <Form.Control
                              type="text"
                              inputMode="numeric"
                              placeholder="1234 5678 9012 3456"
                              maxLength={19}
                              value={cardDetails.cardNumber}
                              onChange={(event) =>
                                setCardDetails({
                                  ...cardDetails,
                                  cardNumber: event.target.value,
                                })
                              }
                              required
                            />
                          </Form.Group>

                          <Row>
                            <Col md={6}>
                              <Form.Group className="mb-3">
                                <Form.Label>Expiry Date</Form.Label>
                                <Form.Control
                                  type="month"
                                  value={cardDetails.expiryDate}
                                  onChange={(event) =>
                                    setCardDetails({
                                      ...cardDetails,
                                      expiryDate: event.target.value,
                                    })
                                  }
                                  required
                                />
                              </Form.Group>
                            </Col>

                            <Col md={6}>
                              <Form.Group className="mb-3">
                                <Form.Label>CVV</Form.Label>
                                <Form.Control
                                  type="password"
                                  inputMode="numeric"
                                  placeholder="123"
                                  maxLength={4}
                                  value={cardDetails.cvv}
                                  onChange={(event) =>
                                    setCardDetails({
                                      ...cardDetails,
                                      cvv: event.target.value,
                                    })
                                  }
                                  required
                                />
                              </Form.Group>
                            </Col>
                          </Row>
                        </div>

                        {/* Student Project Payment Notice */}
                        <div className="alert alert-info mt-3">
                          <strong>Simulated Payment</strong>
                          <p className="mb-0 mt-1">
                            This is a student project demonstration. No real
                            payment will be processed and no actual money will
                            be charged.
                          </p>
                        </div>
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
                        disabled={orderLoading}
                      >
                        {orderLoading ? "Processing Payment..." : "Place Order"}
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
                          Sample prices for the school project. Checkout is not
                          connected yet.
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
