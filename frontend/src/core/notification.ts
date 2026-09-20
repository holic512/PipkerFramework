/**
 * @file notification.ts
 * @project Pipker Framework
 * @module Frontend Notification Infrastructure
 * @description Provides the frontend notification boundary backed by Element Plus Notification.
 * @logic Keeps success, error, warning, and info notifications behind one typed proxy while preserving Element Plus defaults.
 * @dependencies Element Plus
 * @index_tags frontend, notification, element-plus, ui-feedback
 * @author holic512
 */
import { ElNotification, type NotificationHandle } from 'element-plus'

export const notification = {
  success(message: string): NotificationHandle {
    return ElNotification.success(message)
  },
  error(message: string): NotificationHandle {
    return ElNotification.error(message)
  },
  warning(message: string): NotificationHandle {
    return ElNotification.warning(message)
  },
  info(message: string): NotificationHandle {
    return ElNotification.info(message)
  },
} as const
