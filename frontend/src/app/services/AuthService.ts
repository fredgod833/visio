import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LoginRequest {
    login: string;
    password: string;
}

export interface SignupRequest {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
}

export interface UpdateUserRequest {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    newPassword?: string;
}

export interface JwtResponse {
    token: string;
    id: number;
    username: string;
    email: string;
}

export interface User {
    id: number;
    username: string;
    email: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private readonly API_URL = environment.apiUrl;
    private readonly TOKEN_KEY = 'auth_token';
    private readonly USER_KEY = 'current_user';

    private currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
    public currentUser$ = this.currentUserSubject.asObservable();

    private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
    public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

    constructor(private http: HttpClient) {}

    /**
     * Connexion utilisateur
     */
    login(credentials: { username: string; password: string }): Observable<JwtResponse> {
        const loginRequest: LoginRequest = {
            login: credentials.username,
            password: credentials.password
        };

        return this.http.post<JwtResponse>(`${this.API_URL}/auth/login`, loginRequest)
            .pipe(
                tap(response => this.handleAuthResponse(response))
            );

    }

    /**
     * Inscription utilisateur
     */
    register(userData: Omit<any, "confirmPassword">): Observable<JwtResponse> {
        const signupRequest: SignupRequest = {
            firstName: userData['firstName'],
            lastName: userData['lastName'],
            email: userData['email'],
            password: userData['password']
        };

        return this.http.post<JwtResponse>(`${this.API_URL}/auth/register`, signupRequest)
            .pipe(
                tap(response => this.handleAuthResponse(response))
            );
    }

    /**
     * Déconnexion
     */
    logout(): void {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.USER_KEY);
        this.currentUserSubject.next(null);
        this.isAuthenticatedSubject.next(false);
    }

    /**
     * Récupérer le token JWT
     */
    getToken(): string | null {
        return localStorage.getItem(this.TOKEN_KEY);
    }

    /**
     * Récupérer l'utilisateur actuel
     */
    getCurrentUser(): User | null {
        return this.currentUserSubject.value;
    }

    /**
     * Vérifier si l'utilisateur est connecté
     */
    isLoggedIn(): boolean {
        return this.hasToken() && this.currentUserSubject.value !== null;
    }

    /**
     * Récupérer les informations de l'utilisateur depuis le serveur
     */
    getUserProfile(): Observable<JwtResponse> {
        return this.http.get<JwtResponse>(`${this.API_URL}/auth/me`);
    }

    /**
     * Mettre à jour le profil utilisateur
     */
    updateProfile(userData: UpdateUserRequest): Observable<JwtResponse> {
        return this.http.put<JwtResponse>(`${this.API_URL}/auth/me`, userData)
            .pipe(
                tap(response => this.handleAuthResponse(response))
            );
    }

    /**
     * Créer les headers HTTP avec le token
     */
    getAuthHeaders(): HttpHeaders {
        const token = this.getToken();
        return new HttpHeaders({
            'Authorization': token ? `Bearer ${token}` : '',
            'Content-Type': 'application/json'
        });
    }

    /**
     * Gérer la réponse d'authentification
     */
    private handleAuthResponse(response: JwtResponse): void {
        if (response.token) {
            localStorage.setItem(this.TOKEN_KEY, response.token);

            const user: User = {
                id: response.id,
                username: response.username,
                email: response.email
            };

            this.saveUserToStorage(user);
            this.currentUserSubject.next(user);
            this.isAuthenticatedSubject.next(true);
        }
    }

    /**
     * Sauvegarder l'utilisateur dans le localStorage
     */
    private saveUserToStorage(user: User): void {
        localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    }

    /**
     * Récupérer l'utilisateur depuis le localStorage
     */
    private getUserFromStorage(): User | null {
        const userJson = localStorage.getItem(this.USER_KEY);
        if (userJson) {
            try {
                return JSON.parse(userJson);
            } catch {
                return null;
            }
        }
        return null;
    }

    /**
     * Vérifier si un token existe
     */
    private hasToken(): boolean {
        return !!localStorage.getItem(this.TOKEN_KEY);
    }
}
