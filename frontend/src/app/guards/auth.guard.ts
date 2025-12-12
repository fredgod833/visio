import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/AuthService';

/**
 * Guard pour protéger les routes nécessitant une authentification
 * Redirige vers /login si l'utilisateur n'est pas connecté
 */
export const authGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.isLoggedIn()) {
        return true;
    }

    // Rediriger vers login avec l'URL de retour
    router.navigate(['/login'], {
        queryParams: { returnUrl: state.url }
    });

    return false;
};

/**
 * Guard pour les pages publiques (login, register)
 * Redirige vers /chat si déjà connecté
 */
export const publicGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.isLoggedIn()) {
        router.navigate(['/chat']);
        return false;
    }

    return true;
};