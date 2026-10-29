import { Router } from 'express';
import { cadastrarUsuario } from '../controllers/usuarios.controller.js';

export const usuariosRoutes = Router();

usuariosRoutes.post('/', cadastrarUsuario);
