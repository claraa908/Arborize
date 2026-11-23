export function formatCpf(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  return digits
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3}\.\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
}

export function isValidCpf(value: string): boolean {
  const digits = value.replace(/\D/g, '')
  if (digits.length !== 11 || /^([0-9])\1{10}$/.test(digits)) return false

  const calculate = (length: number) => {
    const sum = digits.slice(0, length).split('').reduce(
      (total, digit, index) => total + Number(digit) * (length + 1 - index),
      0
    )
    const remainder = (sum * 10) % 11
    return remainder === 10 ? 0 : remainder
  }

  return calculate(9) === Number(digits[9]) && calculate(10) === Number(digits[10])
}
