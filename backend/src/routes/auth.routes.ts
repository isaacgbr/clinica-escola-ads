import { Router } from 'express';
import { entrar, me, sair } from '../controllers/auth.controller.js';
import { autenticar, protegerCsrf } from '../middlewares/auth.middleware.js';
import { limitesLogin } from '../middlewares/login-limit.middleware.js';

export const authRoutes = Router();

authRoutes.use((_req, res, next) => { res.set('Cache-Control', 'no-store'); next(); });
authRoutes.post('/login', protegerCsrf, limitesLogin.porIp, limitesLogin.porEmailIp, entrar);
authRoutes.get('/me', autenticar, me);
authRoutes.post('/logout', protegerCsrf, autenticar, sair);
