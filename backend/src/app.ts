import express from 'express';
import { routes } from './routes/index.js';
import { errorMiddleware } from './middlewares/error.middleware.js';

export const app = express();

app.use(express.json({ limit: '16kb' }));
app.use(routes);
app.use(errorMiddleware);
