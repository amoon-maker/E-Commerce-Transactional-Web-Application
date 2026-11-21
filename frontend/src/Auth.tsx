import { useState } from 'react'
import type { FormEvent } from 'react'

type AuthProps = {
  onLogin: (user: {
    email: string
    roles: string[]
    vendorId?: number
  }) => void
}

function Auth({ onLogin }: AuthProps) {
  const [isRegister, setIsRegister] = useState(false)
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      // Get CSRF token
      const csrfResponse = await fetch(
        'http://localhost:8080/api/auth/csrf',
        {
          credentials: 'include',
        }
      )

      if (!csrfResponse.ok) {
        throw new Error('Could not connect to the server.')
      }

      const csrfData = await csrfResponse.json()

      // Read CSRF token from the XSRF-TOKEN cookie
      const xsrfCookie = document.cookie
        .split(';')
        .map((cookie) => cookie.trim())
        .find((cookie) => cookie.startsWith('XSRF-TOKEN='))

      const csrfToken = xsrfCookie
        ? decodeURIComponent(
            xsrfCookie.substring('XSRF-TOKEN='.length)
          )
        : null

      if (!csrfToken) {
        throw new Error('CSRF token cookie not found.')
      }

      const endpoint = isRegister ? 'register' : 'login'

      const requestBody = isRegister
        ? {
            firstName,
            lastName,
            email,
            password,
            phone: phone || null,
          }
        : {
            email,
            password,
          }

      const response = await fetch(
        `http://localhost:8080/api/auth/${endpoint}`,
        {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            [csrfData.headerName]: csrfToken,
          },
          body: JSON.stringify(requestBody),
        }
      )

      const responseText = await response.text()

      if (!response.ok) {
        throw new Error(
          responseText || 'Authentication failed.'
        )
      }

      // Registration successful
      if (isRegister) {
        setIsRegister(false)
        setPassword('')
        setError('Registration successful! Please log in.')
        return
      }

      // Login succeeded. Get the authenticated user's information.
      const userResponse = await fetch(
        'http://localhost:8080/api/auth/me',
        {
          credentials: 'include',
        }
      )

      if (!userResponse.ok) {
        throw new Error('Could not retrieve user information.')
      }

      const userData = await userResponse.json()

      onLogin(userData)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Something went wrong. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <h2>{isRegister ? 'Create an Account' : 'Welcome Back'}</h2>

      <p>
        {isRegister
          ? 'Create your Um Hamza customer account.'
          : 'Log in to your Um Hamza account.'}
      </p>

      <form onSubmit={handleSubmit}>
        {isRegister && (
          <>
            <input
              type="text"
              placeholder="First Name"
              value={firstName}
              onChange={(event) => setFirstName(event.target.value)}
              required
            />

            <input
              type="text"
              placeholder="Last Name"
              value={lastName}
              onChange={(event) => setLastName(event.target.value)}
              required
            />

            <input
              type="tel"
              placeholder="Phone Number (Optional)"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
            />
          </>
        )}

        <input
          type="email"
          placeholder="Email Address"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          minLength={8}
        />

        {error && <p className="auth-message">{error}</p>}

        <button type="submit" disabled={loading}>
          {loading
            ? 'Please wait...'
            : isRegister
              ? 'Register'
              : 'Log In'}
        </button>
      </form>

      <p>
        {isRegister
          ? 'Already have an account?'
          : "Don't have an account?"}{' '}

        <button
          type="button"
          className="auth-switch"
          onClick={() => {
            setIsRegister(!isRegister)
            setError('')
          }}
        >
          {isRegister ? 'Log In' : 'Register'}
        </button>
      </p>
    </div>
  )
}

export default Auth
