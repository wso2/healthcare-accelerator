import { createRoot } from 'react-dom/client';
import { OxygenUIThemeProvider, AcrylicOrangeTheme } from '@wso2/oxygen-ui';
import { BrowserRouter } from 'react-router-dom';
import './index.css';
import App from './App.tsx';

// Strip the dark color scheme so MUI never generates the dark-mode CSS variable
// overrides (which are applied via @media (prefers-color-scheme: dark) and
// cannot be overridden programmatically when colorSchemeSelector is 'media').
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const baseTheme = AcrylicOrangeTheme as any;
const lightColorSchemes = { light: baseTheme.colorSchemes?.light };
const lightOnlyTheme = { ...baseTheme, colorSchemes: lightColorSchemes };

// StrictMode intentionally omitted: it double-invokes effects in development,
// which causes two calls to the Asgardeo OauthConsentKey API. That API
// invalidates the sessionDataKeyConsent after first use, breaking the flow.
createRoot(document.getElementById('root')!).render(
  <OxygenUIThemeProvider theme={lightOnlyTheme}>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </OxygenUIThemeProvider>
);
