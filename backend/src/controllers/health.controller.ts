import type { Request, Response } from 'express';

export function healthCheck(_request: Request, response: Response): void {
  response.status(200).json({ status: 'ok' });
}
