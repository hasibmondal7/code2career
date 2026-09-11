export default function Input({ label, ...props }) {
  return <label className="field-label">{label}<input {...props} /></label>
}
