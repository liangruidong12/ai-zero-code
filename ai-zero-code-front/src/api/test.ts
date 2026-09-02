// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** sayhello GET /hello */
export async function sayHello(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/hello', {
    method: 'GET',
    ...(options || {}),
  })
}
