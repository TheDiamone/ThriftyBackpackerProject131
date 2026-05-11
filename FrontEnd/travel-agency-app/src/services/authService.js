import api from './api.js'

function parseUserId(response) {
  const directUserId = Number(
    response?.user_id
    ?? response?.User_ID
    ?? response?.User_Id
    ?? response?.userId
    ?? response?.id
    ?? response?.Id
  )

  if (Number.isInteger(directUserId) && directUserId > 0) {
    return directUserId
  }

  const nestedUserId = Number(
    response?.data?.user_id
    ?? response?.data?.User_ID
    ?? response?.data?.User_Id
    ?? response?.data?.userId
    ?? response?.data?.id
  )

  if (Number.isInteger(nestedUserId) && nestedUserId > 0) {
    return nestedUserId
  }

  return null
}

export const authService = {
  async login(email, password) {
    const response = await api.get('/users/login', {
      params: {
        email,
        password,
      },
      headers: {
        accept: 'application/json',
      },
    })

    const userId = parseUserId(response)

    if (!userId) {
      throw new Error('Login did not return a valid user id.')
    }

    return {
      userId,
      response,
    }
  },
}