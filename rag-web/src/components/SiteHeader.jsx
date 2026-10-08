const LINKS = [
  { href: '#home', label: 'Home' },
  { href: '#about', label: 'About' },
  { href: '#projects', label: 'Projects' },
  { href: '#help', label: 'Help' },
]

const SOCIAL = [
  { name: 'GitHub', icon: GitHubIcon },
  { name: 'LinkedIn', icon: LinkedInIcon },
  { name: 'X', icon: XIcon },
]

export function SiteHeader() {
  return (
    <header className="site-header">
      <nav className="site-nav" aria-label="Primary">
        {LINKS.map((link) => (
          <a key={link.href} href={link.href}>
            {link.label}
          </a>
        ))}
      </nav>
      <ul className="social">
        {SOCIAL.map((item) => (
          <li key={item.name}>
            <button type="button" aria-label={item.name}>
              <item.icon />
            </button>
          </li>
        ))}
      </ul>
    </header>
  )
}

function GitHubIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="currentColor"
        d="M12 2C6.48 2 2 6.58 2 12.26c0 4.52 2.87 8.35 6.84 9.7.5.1.68-.22.68-.48 0-.24-.01-.87-.01-1.7-2.78.62-3.37-1.37-3.37-1.37-.45-1.18-1.11-1.5-1.11-1.5-.91-.64.07-.63.07-.63 1 .07 1.53 1.06 1.53 1.06.9 1.57 2.36 1.12 2.94.86.09-.67.35-1.12.63-1.38-2.22-.26-4.55-1.14-4.55-5.07 0-1.12.39-2.03 1.03-2.75-.1-.26-.45-1.32.1-2.75 0 0 .84-.27 2.75 1.05a9.3 9.3 0 0 1 5 0c1.91-1.32 2.75-1.05 2.75-1.05.55 1.43.2 2.49.1 2.75.64.72 1.03 1.63 1.03 2.75 0 3.94-2.34 4.8-4.57 5.06.36.32.68.94.68 1.9 0 1.37-.01 2.48-.01 2.81 0 .27.18.59.69.48A10.04 10.04 0 0 0 22 12.26C22 6.58 17.52 2 12 2Z"
      />
    </svg>
  )
}

function LinkedInIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="currentColor"
        d="M6.5 9H3.7v11.2h2.8V9ZM5.1 3.8A1.7 1.7 0 1 0 5.1 7a1.7 1.7 0 0 0 0-3.2ZM20.3 20.2h-2.8v-5.8c0-1.6-.6-2.6-2-2.6-1 0-1.6.7-1.9 1.4-.1.2-.1.6-.1.9v6.1H10.7s.04-9.9 0-11.2h2.8v1.8c.4-.6 1.1-1.5 2.8-1.5 2 0 3.6 1.3 3.6 4.2v6.7Z"
      />
    </svg>
  )
}

function XIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="currentColor"
        d="M17.6 3H20.4L14.1 10.1 21.5 21h-5.5l-4.3-6.3L6.7 21H3.9l6.7-7.6L2.7 3h5.6l3.9 5.8L17.6 3Zm-1 16.2h1.5L7.5 4.7H5.9l10.7 14.5Z"
      />
    </svg>
  )
}
