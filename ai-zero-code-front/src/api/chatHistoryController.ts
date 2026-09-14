// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /chatHistory/add */
export async function addChatHistory(
  body: API.ChatHistorySaveRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/chatHistory/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chatHistory/admin/list/page */
export async function adminListChatHistoryByPage(
  body: API.ChatHistoryQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatHistory>('/chatHistory/admin/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chatHistory/list/page */
export async function listChatHistoryVoByPage(
  body: API.ChatHistoryQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatHistoryVO>('/chatHistory/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
