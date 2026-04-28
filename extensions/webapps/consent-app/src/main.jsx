import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ThemeProvider, extendTheme } from '@oxygen-ui/react'
import './index.css'
import App from './App.jsx'

const theme = extendTheme({
  typography: {
    fontFamily: "'Gilmer', 'Segoe UI', Helvetica, Arial, sans-serif",
  },
})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <App />
    </ThemeProvider>
  </StrictMode>,
)
