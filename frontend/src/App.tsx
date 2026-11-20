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
import cheeseImage from "./assets/products/Cheese Thyme.png";
import zatarImage from "./assets/products/Zatar Baladi.png";
import apricotJamImage from "./assets/products/ApricotJam.png";
import mixedPicklesImage from "./assets/products/MixedPickles.png";
import familyQuantityImage from "./assets/products/FamilyQuantityOffer.png";
import frozenSambousekImage from "./assets/products/Frozen Sambousek.png";
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
  image: string;
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

const formatPrice = (amount: number) => {
  return new Intl.NumberFormat("en-AE", {
    style: "currency",
    currency: "AED",
  }).format(amount);
};

function App() {
  const [selectedCategory, setSelectedCategory] =
    useState<Category>("All Products");
  const [searchTerm, setSearchTerm] = useState("");

  const [products, setProducts] = useState<Product[]>([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [productsError, setProductsError] = useState("");
  const [cart, setCart] = useState<CartItem[]>([]);
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
    fetch("http://localhost:8080/api/products")
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
            product.stockQuantity > 0 &&
            product.sku !== "VENDOR-AUTH-TEST-20261117",
        );
        const mappedProducts: Product[] = activeProducts.map((product) => {
          let category: Exclude<Category, "All Products"> =
            "Traditional Pantry";
          let image = familyQuantityImage;
          let description = "Discover traditional flavours from Um Hamza.";

          switch (product.sku) {
            case "UH-CHS-500":
              category = "Dairy & Cheese";
              image = cheeseImage;
              description =
                "Traditional Akkawi cheese, packed in a convenient 500g size.";
              break;

            case "UH-PKL-1000":
              image = mixedPicklesImage;
              description =
                "A traditional mix of pickled vegetables, packed in a 1kg jar.";
              break;

            case "UH-ZTR-250":
              category = "Herbs & Spices";
              image = zatarImage;
              description =
                "A classic zaatar blend, bringing traditional flavour to your table.";
              break;

            case "UH-JAM-400":
              image = apricotJamImage;
              description =
                "Enjoy the fruity sweetness of apricot jam, packed in a 400g jar.";
              break;

            case "UH-SMB-500":
              image = frozenSambousekImage;
              description =
                "Traditional frozen cheese sambousek, ready to prepare and enjoy.";
              break;

            case "UH-OFR-001":
              image = familyQuantityImage;
              description =
                "A convenient family-sized offer for sharing and enjoying together.";
              break;
          }

          return {
            id: product.productId,
            name: product.productName,
            category,
            description,
            price: product.salePrice ?? product.price,
            image,
            stockQuantity: product.stockQuantity,
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

    return matchesCategory && matchesSearch;
  });

  const cartCount = cart.reduce((total, item) => total + item.quantity, 0);

  const cartTotal = cart.reduce(
    (total, item) => total + item.product.price * item.quantity,
    0,
  );

  function addToCart(product: Product) {
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

  function changeQuantity(productId: number, change: number) {
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

      // Get the CSRF token from the cookie
      const csrfToken = document.cookie
        .split("; ")
        .find((cookie) => cookie.startsWith("XSRF-TOKEN="))
        ?.split("=")[1];

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
        ?.split("=")[1];

      await fetch("http://localhost:8080/api/auth/logout", {
        method: "POST",
        credentials: "include",
        headers: {
          [csrfData.headerName]: decodeURIComponent(
            csrfToken || csrfData.token,
          ),
        },
      });

      setLoggedInUser(null);
    } catch (error) {
      console.error("Logout error:", error);
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
                      <Card.Img
                        variant="top"
                        src={product.image}
                        alt={product.name}
                        className="product-image"
                      />
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
                <h3>No products found</h3>
                <p>Try another search term or select a different category.</p>
                <Button
                  className="primary-button"
                  onClick={() => {
                    setSearchTerm("");
                    setSelectedCategory("All Products");
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
                            <img
                              src={item.product.image}
                              alt={item.product.name}
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
                                aria-label={`Increase ${item.product.name} quantity`}
                              >
                                +
                              </Button>
                            </div>

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
                        >
                          Continue to Checkout
                        </Button>

                        <Button
                          variant="link"
                          className="clear-cart-button"
                          onClick={() => setCart([])}
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
