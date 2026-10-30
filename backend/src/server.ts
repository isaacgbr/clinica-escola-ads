import 'dotenv/config';
import { app } from './app.js';
import { configAuth } from './config/auth.js';

configAuth();

const port = Number(process.env.PORT || 3000);

if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error('PORT deve ser um numero inteiro entre 1 e 65535.');
}

app.listen(port, () => {
  console.log(`API da Clinica Escola escutando na porta ${port}.`);
});
