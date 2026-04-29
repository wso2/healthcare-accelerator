import { useState, useEffect, useRef } from 'react';
import asgardeoLogo from './assets/asgardeo-logo.svg';
import Snackbar from '@mui/material/Snackbar';
import Alert from '@mui/material/Alert';
import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  Paper,
  Typography,
  useTheme,
} from '@wso2/oxygen-ui';

export interface Purpose {
  purposeName: string;
  mandatory: boolean;
  purposeDescription?: string;
  elements: string[];
}

export interface ConsentedPurposePayload {
  purposeName: string;
  consentedElements: string[];
}

export interface ConsentPageProps {
  appName?: string;
  purposes: Purpose[];
  loading?: boolean;
  error?: string | null;
  sessionDataKeyConsent?: string;
  spId?: string;
  initialConsentedPurposeNames?: string[];
  initialConsentedElements?: Record<string, string[]>;
  submitting?: 'allow' | 'deny' | null;
  submitError?: string | null;
  onClearSubmitError?: () => void;
  onAllow?: (consentedPurposes: ConsentedPurposePayload[]) => void;
  onDeny?: () => void;
}

export default function ConsentPage({
  appName = 'MyHealthApp',
  purposes,
  loading = false,
  error = null,
  submitting = null,
  submitError = null,
  onClearSubmitError,
  initialConsentedPurposeNames,
  initialConsentedElements,
  onAllow,
  onDeny,
}: ConsentPageProps) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const theme = useTheme() as any;

  // purposeName → elementName → boolean
  // When elements is empty (showConsentElements=false), uses the sentinel key '__purpose__'
  // to track the purpose-level checked state.
  const PURPOSE_KEY = '__purpose__';
  const [checked, setChecked] = useState<Record<string, Record<string, boolean>>>({});
  const initialApplied = useRef(false);

  useEffect(() => {
    if (initialApplied.current || purposes.length === 0) return;
    initialApplied.current = true;

    const init: Record<string, Record<string, boolean>> = {};
    for (const p of purposes) {
      if (p.mandatory) continue;
      const prevElems = initialConsentedElements?.[p.purposeName];
      const wasPreviouslyConsented = initialConsentedPurposeNames?.includes(p.purposeName);
      if (p.elements.length === 0) {
        init[p.purposeName] = { [PURPOSE_KEY]: wasPreviouslyConsented ?? false };
      } else if (prevElems) {
        init[p.purposeName] = Object.fromEntries(p.elements.map(el => [el, prevElems.includes(el)]));
      } else if (wasPreviouslyConsented) {
        init[p.purposeName] = Object.fromEntries(p.elements.map(el => [el, true]));
      }
    }
    setChecked(init);
  }, [purposes, initialConsentedPurposeNames, initialConsentedElements]);

  const isElementChecked = (purposeName: string, el: string) =>
    checked[purposeName]?.[el] ?? false;

  const isPurposeFullyChecked = (p: Purpose) => {
    if (p.mandatory) return true;
    if (p.elements.length === 0) return checked[p.purposeName]?.[PURPOSE_KEY] ?? false;
    return p.elements.every(el => isElementChecked(p.purposeName, el));
  };

  const isPurposeIndeterminate = (p: Purpose) => {
    if (p.mandatory || p.elements.length === 0) return false;
    return p.elements.some(el => isElementChecked(p.purposeName, el)) &&
      !p.elements.every(el => isElementChecked(p.purposeName, el));
  };

  const handlePurposeToggle = (p: Purpose) => {
    if (p.mandatory) return;
    if (p.elements.length === 0) {
      const current = checked[p.purposeName]?.[PURPOSE_KEY] ?? false;
      setChecked(prev => ({ ...prev, [p.purposeName]: { [PURPOSE_KEY]: !current } }));
      return;
    }
    const allChecked = isPurposeFullyChecked(p);
    setChecked(prev => ({
      ...prev,
      [p.purposeName]: Object.fromEntries(p.elements.map(el => [el, !allChecked])),
    }));
  };

  const handleElementToggle = (purposeName: string, el: string) => {
    setChecked(prev => ({
      ...prev,
      [purposeName]: { ...prev[purposeName], [el]: !(prev[purposeName]?.[el] ?? false) },
    }));
  };

  const handleAllow = () => {
    const consentedPurposes: ConsentedPurposePayload[] = purposes
      .filter(p => p.mandatory || isPurposeFullyChecked(p) || isPurposeIndeterminate(p))
      .map(p => ({
        purposeName: p.purposeName,
        consentedElements: p.elements.filter(el =>
          p.mandatory || isElementChecked(p.purposeName, el)
        ),
      }));
    onAllow?.(consentedPurposes);
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <img src={asgardeoLogo} alt="Asgardeo" style={{ height: 40 }} />
      </Box>

      {loading && (
        <CircularProgress size={36} />
      )}

      {!loading && error && (
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <Typography sx={{ fontSize: '3rem', mb: 2, lineHeight: 1 }}>😔</Typography>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
            Oops, something went wrong.
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Please try again later or contact support.
          </Typography>
        </Box>
      )}

      {!loading && !error && <Paper sx={{ p: 4, maxWidth: 440, width: '100%' }}>
        {/* Title */}
        <Typography variant="h5" sx={{ fontWeight: 700, mb: 3, lineHeight: 1.3, textAlign: 'center' }}>
          <Typography component="span" variant="h5" sx={{ fontWeight: 700, color: 'warning.main' }}>
            {appName}
          </Typography>
          {' '}wants to access your account.
        </Typography>

        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          This will allow application to:
        </Typography>

        {/* Purposes */}
        <Box sx={{ bgcolor: 'background.default', borderRadius: 2, p: 2, mb: 3, minHeight: 64 }}>
          {purposes.map((purpose, idx) => (
            <Box key={purpose.purposeName} sx={{ mb: idx < purposes.length - 1 ? 2 : 0 }}>
              {/* Purpose row */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                <Box
                  sx={{
                    width: 8,
                    height: 8,
                    borderRadius: '50%',
                    background: theme.gradient?.primary,
                    flexShrink: 0,
                  }}
                />
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={isPurposeFullyChecked(purpose)}
                      indeterminate={isPurposeIndeterminate(purpose)}
                      disabled={purpose.mandatory}
                      onChange={() => handlePurposeToggle(purpose)}
                      size="small"
                      sx={{ p: '2px 8px 2px 4px' }}
                    />
                  }
                  label={
                    <>
                      <Typography
                        variant="body2"
                        sx={{
                          fontWeight: 600,
                          color: purpose.mandatory ? 'text.disabled' : 'text.primary',
                        }}
                      >
                        {purpose.purposeName}
                        {purpose.mandatory && (
                          <Typography component="span" sx={{ color: 'error.main', ml: '2px', fontWeight: 600 }}>
                            *
                          </Typography>
                        )}
                      </Typography>
                      {purpose.purposeDescription && (
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          sx={{ display: 'block', lineHeight: 1.4 }}
                        >
                          {purpose.purposeDescription}
                        </Typography>
                      )}
                    </>
                  }
                  sx={{ m: 0 }}
                />
              </Box>

              {/* Element rows */}
              <Box sx={{ pl: 4 }}>
                {purpose.elements.map(el => (
                  <Box key={el} sx={{ display: 'flex', alignItems: 'center' }}>
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={purpose.mandatory || isElementChecked(purpose.purposeName, el)}
                          disabled={purpose.mandatory}
                          onChange={() => !purpose.mandatory && handleElementToggle(purpose.purposeName, el)}
                          size="small"
                          sx={{ p: '2px 8px 2px 4px' }}
                        />
                      }
                      label={
                        <Typography
                          variant="caption"
                          sx={{ color: purpose.mandatory ? 'text.disabled' : 'text.secondary' }}
                        >
                          {el}
                        </Typography>
                      }
                      sx={{ m: 0 }}
                    />
                  </Box>
                ))}
              </Box>
            </Box>
          ))}
        </Box>

        {/* Footer text */}
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ display: 'block', mb: 3, textAlign: 'center', lineHeight: 1.5 }}
        >
          By clicking 'Allow', you agree to share the data related to the above selections with {appName}.
        </Typography>

        {/* Buttons */}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <Button
            variant="contained"
            fullWidth
            onClick={handleAllow}
            disabled={submitting !== null}
            sx={{
              borderRadius: '50px',
              textTransform: 'none',
              fontWeight: 600,
              fontSize: '0.95rem',
              py: 1.25,
            }}
          >
            {submitting === 'allow' ? <CircularProgress size={20} sx={{ color: 'inherit' }} /> : 'Allow'}
          </Button>

          <Button
            variant="outlined"
            fullWidth
            onClick={onDeny}
            disabled={submitting !== null}
            sx={{
              borderRadius: '50px',
              textTransform: 'none',
              fontWeight: 600,
              fontSize: '0.95rem',
              py: 1.25,
              borderColor: 'divider',
              color: 'text.secondary',
              '&:hover': { bgcolor: 'action.hover', borderColor: 'divider' },
            }}
          >
            {submitting === 'deny' ? <CircularProgress size={20} sx={{ color: 'inherit' }} /> : 'Deny'}
          </Button>
        </Box>
      </Paper>}


      <Snackbar
        open={!!submitError}
        autoHideDuration={6000}
        onClose={onClearSubmitError}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={onClearSubmitError} severity="error" sx={{ width: '100%' }}>
          {submitError}
        </Alert>
      </Snackbar>
    </Box>
  );
}
