export const USER_ROLES = [
  'ROLE_CIDADAO',
  'ROLE_ADMINISTRADOR_AMMA',
  'ROLE_FUNCIONARIO_AMMA',
  'ROLE_AMMA',
  'ROLE_GESTOR_IFCE',
  'ROLE_IFCE_GESTOR',
  'ROLE_IFCE_BOLSISTA'
] as const

export type UserRole = typeof USER_ROLES[number]

export function isUserRole(value: string | null): value is UserRole {
  return USER_ROLES.some((role) => role === value)
}
