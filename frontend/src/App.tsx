
import { useEffect, useMemo, useState } from 'react'
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
} from 'react-bootstrap'

import logo from './assets/umhamzalogo.png'
import cheeseImage from './assets/products/Cheese Thyme.png'
import labanaImage from './assets/products/Labana Shatta.png'
import sumacImage from './assets/products/Sumac Baladi.png'
import zatarImage from './assets/products/Zatar Baladi.png'
import molasseImage from './assets/products/Zatar Pomegranate Molasse.png'
import groupImage from './assets/groupummhamza.png'

import './App.css'

type Category =
  | 'All Products'
  | 'Dairy & Cheese'
  | 'Traditional Pantry'
  | 'Herbs & Spices'

type Product = {
  id: number
  name: string
  category: Exclude<Category, 'All Products'>
  description: string
  price: number
  image: string
}

type CartItem = {
  product: Product
  quantity: number
}

function App() {
  const [selectedCategory, setSelectedCategory] =
    useState<Category>('All Products')
  const [searchTerm, setSearchTerm] = useState('')
  const [cart, setCart] = useState<CartItem[]>([])
  const [cartOpen, setCartOpen] = useState(false)
  const [checkoutOpen, setCheckoutOpen] = useState(false)
const [orderConfirmed, setOrderConfirmed] = useState(false)
const [darkMode, setDarkMode] = useState(false)
  const [customer, setCustomer] = useState({
    name: '',
    email: '',
    phone: '',
    address: '',
  })
useEffect(() => {
  if (cartOpen) {
    document.getElementById('shopping-cart')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
  }
}, [cartOpen])
  const products: Product[] = [
    {
      id: 1,
      name: 'Cheese Thyme',
      category: 'Dairy & Cheese',
      description: 'A traditional cheese and thyme combination.',
      price: 8.99,
      image: cheeseImage,
    },
    {
      id: 2,
      name: 'Labana Shatta',
      category: 'Dairy & Cheese',
      description: 'A traditional labneh product with a spicy twist.',
      price: 7.99,
      image: labanaImage,
    },
    {
      id: 3,
      name: 'Sumac Baladi',
      category: 'Herbs & Spices',
      description: 'A traditional pantry essential with a tangy flavour.',
      price: 5.99,
      image: sumacImage,
    },
    {
      id: 4,
      name: 'Zatar Baladi',
      category: 'Herbs & Spices',
      description: 'A classic Middle Eastern seasoning.',
      price: 6.49,
      image: zatarImage,
    },
    {
      id: 5,
      name: 'Zatar Pomegranate Molasse',
      category: 'Traditional Pantry',
      description: 'A distinctive combination inspired by traditional flavours.',
      price: 7.49,
      image: molasseImage,
    },
  ]

  const categories: Category[] = [
    'All Products',
    'Dairy & Cheese',
    'Traditional Pantry',
    'Herbs & Spices',
  ]

  const filteredProducts = useMemo(() => {
    return products.filter((product) => {
      const matchesCategory =
        selectedCategory === 'All Products' ||
        product.category === selectedCategory

      const matchesSearch = product.name
        .toLowerCase()
        .includes(searchTerm.trim().toLowerCase())

      return matchesCategory && matchesSearch
    })
  }, [selectedCategory, searchTerm])

  const cartCount = cart.reduce(
    (total, item) => total + item.quantity,
    0,
  )

  const cartTotal = cart.reduce(
    (total, item) => total + item.product.price * item.quantity,
    0,
  )

  function addToCart(product: Product) {
    setCart((currentCart) => {
      const existingItem = currentCart.find(
        (item) => item.product.id === product.id,
      )

      if (existingItem) {
        return currentCart.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item,
        )
      }

      return [...currentCart, { product, quantity: 1 }]
    })
  }

  function changeQuantity(productId: number, change: number) {
    setCart((currentCart) =>
      currentCart
        .map((item) =>
          item.product.id === productId
            ? { ...item, quantity: item.quantity + change }
            : item,
        )
        .filter((item) => item.quantity > 0),
    )
  }

  function handleCheckout(event: React.FormEvent<HTMLFormElement>) {
  event.preventDefault()
  setOrderConfirmed(true)
}

  return (
    <div className={`storefront ${darkMode ? 'dark-mode' : ''}`}>
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
  aria-label={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}
>
  {darkMode ? '☀' : '☾'}
</Button>
            <Button
              className="cart-button"
              onClick={() => {
                setCartOpen(!cartOpen)
                setCheckoutOpen(false)
              }}
              aria-expanded={cartOpen}
            >
              Cart <Badge bg="light" text="dark">{cartCount}</Badge>
            </Button>
          </Navbar.Collapse>
        </Container>
      </Navbar>

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
                  Discover traditional flavours inspired by the food,
                  culture, and hospitality that bring us together.
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
              <h2>Good food, <span>good memories.</span></h2>
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
                        ? 'category-button active'
                        : 'category-button'
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

                      <div className="product-card-footer mt-auto">
  <span className="product-price">
    ${product.price.toFixed(2)}
  </span>

  {cart.find((item) => item.product.id === product.id) ? (
    <div className="product-quantity-controls">
      <Button
        onClick={() => changeQuantity(product.id, -1)}
        aria-label={`Decrease ${product.name} quantity`}
      >
        −
      </Button>

      <span>
        {cart.find((item) => item.product.id === product.id)?.quantity} in cart
      </span>

      <Button
        onClick={() => changeQuantity(product.id, 1)}
        aria-label={`Increase ${product.name} quantity`}
      >
        +
      </Button>
    </div>
  ) : (
    <Button
      className="add-button"
      onClick={() => addToCart(product)}
      aria-label={`Add ${product.name} to cart`}
    >
      + Add
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
                    setSearchTerm('')
                    setSelectedCategory('All Products')
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

    <span className="eyebrow">CHECKOUT COMPLETE</span>

    <h2>Thank you, {customer.name}!</h2>

    <p>
      Your checkout demonstration has been completed.
    </p>

    <p className="confirmation-notice">
      This is a frontend demonstration. No real order has
      been placed or saved.
    </p>

    <h3>Order Summary</h3>

    {cart.map((item) => (
      <div className="checkout-summary-item" key={item.product.id}>
        <span>
          {item.product.name} × {item.quantity}
        </span>
        <strong>
          ${(item.product.price * item.quantity).toFixed(2)}
        </strong>
      </div>
    ))}

    <div className="checkout-summary-total">
      <span>Order Total</span>
      <strong>${cartTotal.toFixed(2)}</strong>
    </div>

    <Button
  className="primary-button"
  onClick={() => {
    setOrderConfirmed(false)
    setCheckoutOpen(false)
    setCartOpen(false)
    setCart([])
    setCustomer({
      name: '',
      email: '',
      phone: '',
      address: '',
    })
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
    <div className="checkout-summary-item" key={item.product.id}>
      <span>
        {item.product.name} × {item.quantity}
      </span>

      <strong>
        ${(item.product.price * item.quantity).toFixed(2)}
      </strong>
    </div>
  ))}

  <div className="checkout-summary-total">
    <span>Subtotal</span>
    <strong>${cartTotal.toFixed(2)}</strong>
  </div>
</div>

<Form onSubmit={handleCheckout}>
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

                      <div className="cart-total">
                        <span>Order Total</span>
                        <strong>${cartTotal.toFixed(2)}</strong>
                      </div>

                      <Button type="submit" className="primary-button">
                        Place Order
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
                                ${item.product.price.toFixed(2)} each
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
                              $
                              {(item.product.price * item.quantity).toFixed(2)}
                            </strong>
                          </div>
                        ))}

                        <div className="cart-total">
                          <span>Subtotal</span>
                          <strong>${cartTotal.toFixed(2)}</strong>
                        </div>

                        <p className="cart-note">
                          Sample prices for the school project. Checkout is
                          not connected yet.
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
              At Um Hamza, food is more than what we share at the table.
              It is a connection to our roots, our memories, and the people
              we love.
            </p>
          </Container>
        </section>
      </main>

      <footer className="store-footer">
        <Container>
          <div className="footer-content">
            <span>
              © {new Date().getFullYear()} Um Hamza Foodstuff Factory
            </span>
          </div>
        </Container>
      </footer>
    </div>
  )
}

export default App
