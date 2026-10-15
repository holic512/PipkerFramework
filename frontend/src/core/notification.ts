/**
 * @file notification.ts
 * @project Pipker Framework
 * @module Frontend Notification Infrastructure
 * @description Provides the frontend notification boundary backed by Element Plus Notification.
 * @logic Keeps success, error, warning, and info notifications behind one typed proxy with explicit styles and Element Plus defaults; callers own feedback and HTTP remains silent.
 * @dependencies Element Plus
 * @index_tags frontend, notification, element-plus, ui-feedback
 * @author holic512
 */
import { ElNotification, type NotificationHandle } from 'element-plus'
import 'element-plus/es/components/notification/style/css'

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
