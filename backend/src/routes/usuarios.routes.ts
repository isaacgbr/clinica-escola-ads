import { Router } from 'express';
import { cadastrarUsuario } from '../controllers/usuarios.controller.js';
import { limiteCadastro } from '../middlewares/cadastro-limit.middleware.js';

export const usuariosRoutes = Router();

usuariosRoutes.post('/', limiteCadastro, cadastrarUsuario);
