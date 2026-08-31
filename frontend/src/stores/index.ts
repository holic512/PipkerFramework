/**
 * @file index.ts
 * @project Pipker Framework
 * @module State management bootstrap
 * @description Exports the application-wide Pinia instance.
 * @logic Creates one Pinia root that feature stores receive through Vue plugin registration.
 * @dependencies Pinia
 * @index_tags frontend,pinia,state,bootstrap
 * @author holic512
 */
import { createPinia } from 'pinia'

export const pinia = createPinia()
