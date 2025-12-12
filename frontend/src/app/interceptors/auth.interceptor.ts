import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/AuthService';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

/**
 * Intercepteur HTTP pour ajouter automatiquement le token JWT
 * aux requêtes sortantes et gérer les erreurs d'authentification
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    // Récupérer le token
    const token = authService.getToken();

    // Cloner la requête et ajouter le header Authorization si token existe
    let authReq = req;
    if (token) {
        authReq = req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`
            }
        });
    }

    // Gérer les erreurs de réponse
    return next(authReq).pipe(
        catchError(error => {
            // Si erreur 401 (non autorisé), déconnecter l'utilisateur
            if (error.status === 401) {
                authService.logout();
                router.navigate(['/login']);
            }

            // Si erreur 403 (interdit), rediriger vers page d'accès refusé
            if (error.status === 403) {
                router.navigate(['/forbidden']);
            }

            return throwError(() => error);
        })
    );
};