import { Router } from 'express';
import { healthCheck } from '../controllers/health.controller.js';
import { usuariosRoutes } from './usuarios.routes.js';
import { authRoutes } from './auth.routes.js';

export const routes = Router();

routes.get('/health', healthCheck);
routes.use('/usuarios', usuariosRoutes);
routes.use('/auth', authRoutes);
