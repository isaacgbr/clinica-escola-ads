import { Router } from 'express';
import { healthCheck } from '../controllers/health.controller.js';

export const routes = Router();

routes.get('/health', healthCheck);
