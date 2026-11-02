export function formatBrazilianPhone(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  if (digits.length === 0) return ''
  if (digits.length <= 2) return `(${digits}`

  const areaCode = digits.slice(0, 2)
  const subscriber = digits.slice(2)

  if (digits.length <= 6) return `(${areaCode}) ${subscriber}`

  const subscriberLength = digits.length > 10 ? 5 : 4
  return `(${areaCode}) ${subscriber.slice(0, subscriberLength)}-${subscriber.slice(subscriberLength)}`
}
