import { useEffect, useState } from 'react';
import { Routes, Route, Navigate, useSearchParams } from 'react-router-dom';
import ConsentPage, { type Purpose, type ConsentedPurposePayload } from './ConsentPage';

const BFF_URL = import.meta.env.VITE_CONSENT_BFF_URL ?? '';
const ASGARDEO_AUTHORIZE_URL = import.meta.env.VITE_ASGARDEO_AUTHORIZE_URL ?? '';

function submitAsgardeoConsent(sessionDataKeyConsent: string, approved = true) {
  if (!ASGARDEO_AUTHORIZE_URL) {
    throw new Error('VITE_ASGARDEO_AUTHORIZE_URL is not set');
  }
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = ASGARDEO_AUTHORIZE_URL;

  const fields: Record<string, string> = {
    hasApprovedAlways: 'false',
    sessionDataKeyConsent,
    consent: approved ? 'approve' : 'deny',
  };

  for (const [name, value] of Object.entries(fields)) {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
  }

  document.body.appendChild(form);
  form.submit();
}

function ConsentRoute() {
  const [searchParams] = useSearchParams();
  const sessionDataKeyConsent = searchParams.get('sessionDataKeyConsent') ?? '';
  const spId = searchParams.get('spId') ?? '';

  const [appName, setAppName] = useState('');
  const [consentToken, setConsentToken] = useState('');
  const [purposes, setPurposes] = useState<Purpose[]>([]);
  const [existingConsentId, setExistingConsentId] = useState<string | undefined>();
  const [previouslyConsentedPurposeNames, setPreviouslyConsentedPurposeNames] = useState<string[]>([]);
  const [previouslyConsentedElements, setPreviouslyConsentedElements] = useState<Record<string, string[]>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState<'allow' | 'deny' | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!BFF_URL) {
      setError('VITE_CONSENT_BFF_URL is not set');
      setLoading(false);
      return;
    }
    const params = new URLSearchParams({ sessionDataKeyConsent, spId });
    fetch(`${BFF_URL}/v1/get-consent-data?${params}`)
      .then((res) => {
        if (!res.ok) throw new Error(`BFF error: ${res.status}`);
        return res.json() as Promise<{ appName: string; consentToken: string; purposes: Purpose[]; existingConsentId?: string; previouslyConsentedPurposeNames?: string[]; previouslyConsentedElements?: Record<string, string[]> }>;
      })
      .then(({ appName: name, consentToken: token, purposes: list, existingConsentId: id, previouslyConsentedPurposeNames: prev, previouslyConsentedElements: prevElems }) => {
        setAppName(name);
        setConsentToken(token);
        setPurposes(list);
        setExistingConsentId(id);
        setPreviouslyConsentedPurposeNames(prev ?? []);
        setPreviouslyConsentedElements(prevElems ?? {});
      })
      .catch((err: unknown) => {
        console.error('Failed to load consent data', err);
        setError('Failed to load consent data. Please try again.');
      })
      .finally(() => setLoading(false));
  }, [sessionDataKeyConsent, spId]);

  const handleAllow = async (consentedPurposes: ConsentedPurposePayload[]) => {
    setSubmitting('allow');
    setSubmitError(null);
    const body = {
      sessionDataKeyConsent,
      spId,
      consentToken,
      approved: true,
      consentedPurposes,
      ...(existingConsentId ? { existingConsentId } : {}),
    };
    try {
      if (!BFF_URL) throw new Error('VITE_CONSENT_BFF_URL is not set');
      const res = await fetch(`${BFF_URL}/v1/submit-consent`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (!res.ok) throw new Error(`BFF error: ${res.status}`);
      await res.json();
      submitAsgardeoConsent(sessionDataKeyConsent);
    } catch (err) {
      console.error('Failed to submit consent', err);
      setSubmitError('Failed to submit consent. Please try again.');
      setSubmitting(null);
    }
    // No finally — on success, buttons stay disabled while page redirects
  };

  const handleDeny = () => {
    setSubmitting('deny');
    try {
      submitAsgardeoConsent(sessionDataKeyConsent, false);
    } catch (err) {
      console.error('Failed to submit consent', err);
      setSubmitError('Failed to submit consent. Please try again.');
      setSubmitting(null);
    }
  };

  return (
    <ConsentPage
      appName={appName}
      sessionDataKeyConsent={sessionDataKeyConsent}
      spId={spId}
      purposes={purposes}
      loading={loading}
      error={error}
      initialConsentedPurposeNames={previouslyConsentedPurposeNames}
      initialConsentedElements={previouslyConsentedElements}
      submitting={submitting}
      submitError={submitError}
      onClearSubmitError={() => setSubmitError(null)}
      onAllow={handleAllow}
      onDeny={handleDeny}
    />
  );
}

function App() {
  return (
    <Routes>
      <Route path="/consent-page" element={<ConsentRoute />} />
      <Route path="*" element={<Navigate to="/consent-page" replace />} />
    </Routes>
  );
}

export default App;
