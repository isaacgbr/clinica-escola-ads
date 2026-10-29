import { Router } from 'express';
import { healthCheck } from '../controllers/health.controller.js';
import { usuariosRoutes } from './usuarios.routes.js';

export const routes = Router();

routes.get('/health', healthCheck);
routes.use('/usuarios', usuariosRoutes);
