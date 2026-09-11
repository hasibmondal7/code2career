export const authSchema = {
  email: { required: true },
  password: { required: true, minLength: 8 },
}
