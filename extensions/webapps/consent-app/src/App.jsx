import { useState, useEffect } from 'react';
import ConsentPage from './ConsentPage';
import PatientPickerPage from './PatientPickerPage';

function readStoredPatient() {
  const stored = sessionStorage.getItem("selectedPatient");
  if (!stored) return null;
  sessionStorage.removeItem("selectedPatient");
  try { return JSON.parse(stored); } catch { return null; }
}

function isPractitioner(scimUser) {
  const fhirUser = scimUser?.["urn:scim:schemas:extension:custom:User"]?.fhirUser ?? "";
  return typeof fhirUser === "string" && fhirUser.includes("Practitioner");
}

export default function App() {
  const [consentProps, setConsentProps] = useState(null);
  const [consentError, setConsentError] = useState(null);
  const [selectedPatient, setSelectedPatient] = useState(readStoredPatient);
  const [practitioner, setPractitioner] = useState(null); // null = loading, true/false = resolved

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const sessionDataKeyConsent = params.get("sessionDataKeyConsent");
    const spId = params.get("spId");

    if (!sessionDataKeyConsent || !spId) {
      setConsentError("Missing required query parameters: sessionDataKeyConsent and spId.");
      return;
    }

    fetch(`/api/consent-context?sessionDataKeyConsent=${encodeURIComponent(sessionDataKeyConsent)}&spId=${encodeURIComponent(spId)}`)
      .then((r) => r.ok ? r.json() : r.text().then((msg) => Promise.reject(msg || r.statusText)))
      .then((data) => setConsentProps(data))
      .catch((err) => setConsentError(String(err)));
  }, []);

  const userId = consentProps?.user ?? "";

  useEffect(() => {
    if (consentProps === null) return; // wait for consent context
    if (!userId) {
      setPractitioner(false);
      return;
    }
    fetch(`/api/me?userId=${encodeURIComponent(userId)}`)
      .then((r) => r.ok ? r.json() : Promise.reject())
      .then((data) => setPractitioner(isPractitioner(data)))
      .catch(() => setPractitioner(false));
  }, [userId, consentProps]);

  if (consentError) {
    return (
      <div style={{ fontFamily: 'monospace', padding: '2rem', color: 'var(--oxygen-palette-error-main, #d32f2f)' }}>
        <strong>Failed to load consent context.</strong> {consentError}
      </div>
    );
  }

  if (consentProps === null || practitioner === null) {
    return null; // loading
  }

  if (practitioner && !selectedPatient) {
    return (
      <PatientPickerPage
        {...consentProps}
        onProceed={(patient) => setSelectedPatient(patient)}
      />
    );
  }

  const additionalContext = selectedPatient ? [JSON.stringify(selectedPatient)] : [];
  return <ConsentPage {...consentProps} additionalContext={additionalContext} />;
}
