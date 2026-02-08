import {Component, OnInit, OnDestroy, ViewChild, ElementRef, signal, NgZone, HostListener} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { WebSocketService, ChatMessage } from '../../services/WebSocketService';
import { WebRTCService } from '../../services/WebRTCService';
import { AuthService } from '../../services/AuthService';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss']
})
export class ChatComponent implements OnInit, OnDestroy {
  @ViewChild('localVideo') localVideo!: ElementRef<HTMLVideoElement>;
  @ViewChild('remoteVideo') remoteVideo!: ElementRef<HTMLVideoElement>;
  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  // ✅ Transformer messages en Signal aussi
  messages = signal<ChatMessage[]>([]);
  newMessage: string = '';
  username: string = '';
  otherUser: string = '';
  roomId: string = 'general';

  // ✅ SOLUTION: Utiliser un Signal au lieu d'une simple variable
  isConnected = signal(false);

  isVideoCallActive: boolean = false;
  isAudioEnabled: boolean = true;
  isVideoEnabled: boolean = true;

  remotePeerUsername: string = '';
  showVideoCallDialog: boolean = false;
  incomingCallFrom: string = '';

  private subscriptions: Subscription[] = [];

  constructor(
    private wsService: WebSocketService,
    private webRTCService: WebRTCService,
    private authService: AuthService,
    private router: Router,
    private ngZone: NgZone  // ✅ Ajouter NgZone
  ) {}

  ngOnInit(): void {
    // Récupérer l'utilisateur connecté
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      console.error('Aucun utilisateur connecté, redirection vers login');
      this.router.navigate(['/login']);
      return;
    }

    this.username = currentUser.username;
    this.roomId = currentUser.username;
    console.log('Chat initialisé pour:', this.username);

    // ✅ S'abonner aux messages avec Signal
    this.subscriptions.push(
      this.wsService.messages$.subscribe(messages => {
        console.log('📨 Nouveaux messages reçus:', messages.length);
        this.messages.set(messages);  // ✅ Utiliser .set() pour le Signal
        this.scrollToBottom();
      })
    );

    // ✅ S'abonner à l'état de connexion avec Signal
    this.subscriptions.push(
      this.wsService.connected$.subscribe(connected => {
        console.log('🔄 Changement status WebSocket:', connected);
        this.isConnected.set(connected);  // ✅ Utiliser .set() pour mettre à jour le Signal
        console.log('✅ Signal mis à jour:', this.isConnected());
      })
    );

    // Se connecter au WebSocket APRÈS les souscriptions
    this.wsService.connect(this.username);

    // Écouter les appels entrants
    window.addEventListener('incomingCall', this.handleIncomingCall.bind(this));

    // Écouter le stream distant
    window.addEventListener('remoteStream', this.handleRemoteStream.bind(this));
  }


  ngOnDestroy(): void {
    this.wsService.disconnect(this.username);
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.isVideoCallActive) {
      this.webRTCService.stopCall();
    }
  }

  sendMessage(): void {
    // ✅ Utiliser .() pour lire un Signal
    if (this.newMessage.trim() && this.isConnected()) {
      const message: {
        sender: string;
        customerId: number;
        agencyId: number;
        type: string;
        content: string;
        timestamp: Date
      } = {
        sender: this.username,
        content: this.newMessage,
        agencyId:1,
        customerId:1,
        type: 'CHAT',
        timestamp: new Date()
      };

      this.wsService.sendMessage(message);
      this.newMessage = '';
    }
  }

  setOther(targetUsername : string): void {
    this.otherUser = targetUsername;
  }

  async startVideoCall(): Promise<void> {
    this.remotePeerUsername = this.otherUser;

    // ✅ CORRECTION: Activer la vidéo AVANT d'accéder aux éléments
    this.isVideoCallActive = true;

    try {
      // ✅ Attendre que Angular rende les éléments vidéo
      await new Promise(resolve => setTimeout(resolve, 100));

      console.log('🎬 Démarrage appel vidéo avec', this.otherUser);

      // Démarrer le stream local (maintenant les éléments existent)
      await this.webRTCService.startLocalStream(
        this.localVideo.nativeElement
      );

      // Initier l'appel
      this.wsService.initiateCall(this.otherUser, this.roomId);

      // Créer l'offre WebRTC
      await this.webRTCService.initiateCall(this.otherUser);

      console.log('✅ Appel vidéo démarré avec succès');
    } catch (error: any) {
      console.error('❌ Erreur démarrage appel vidéo:', error);

      // ✅ Afficher un message d'erreur à l'utilisateur
      alert(error.message || 'Impossible de démarrer l\'appel vidéo. Vérifiez vos permissions caméra/micro.');

      this.isVideoCallActive = false;
    }
  }

  async handleIncomingCall(event: any): Promise<void> {
    // ✅ CORRECTION: Exécuter dans la zone Angular pour forcer le rafraîchissement
    this.ngZone.run(() => {
      const { from } = event.detail;
      this.incomingCallFrom = from;
      this.showVideoCallDialog = true;
      console.log('📞 Appel entrant de', from);
    });
  }

  async acceptCall(): Promise<void> {
    this.showVideoCallDialog = false;
    this.remotePeerUsername = this.incomingCallFrom;

    // ✅ CORRECTION: Activer la vidéo AVANT d'accéder aux éléments
    this.isVideoCallActive = true;

    try {
      // ✅ Attendre que Angular rende les éléments vidéo
      await new Promise(resolve => setTimeout(resolve, 100));

      console.log('📞 Acceptation appel de', this.incomingCallFrom);

      await this.webRTCService.startLocalStream(
        this.localVideo.nativeElement
      );

      console.log('✅ Appel accepté avec succès');
    } catch (error: any) {
      console.error('❌ Erreur acceptation appel:', error);

      // ✅ Afficher un message d'erreur
      alert(error.message || 'Impossible d\'accepter l\'appel. Vérifiez vos permissions caméra/micro.');

      this.isVideoCallActive = false;
    }
  }

  rejectCall(): void {
    this.showVideoCallDialog = false;
    this.incomingCallFrom = '';
  }

  handleRemoteStream(event: any): void {
    // ✅ CORRECTION: Exécuter dans la zone Angular
    this.ngZone.run(() => {
      const stream = event.detail;
      this.remoteVideo.nativeElement.srcObject = stream;
      console.log('📹 Stream distant reçu');
    });
  }

  endCall(): void {
    this.webRTCService.stopCall();
    this.isVideoCallActive = false;
    this.remotePeerUsername = '';
  }

  toggleAudio(): void {
    this.isAudioEnabled = !this.isAudioEnabled;
    this.webRTCService.toggleAudio(this.isAudioEnabled);
  }

  toggleVideo(): void {
    this.isVideoEnabled = !this.isVideoEnabled;
    this.webRTCService.toggleVideo(this.isVideoEnabled);
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop =
          this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }

  logout(): void {
    this.wsService.disconnect(this.username);
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  @HostListener('window:beforeunload')
  beforeunloadHandler() {
    this.wsService.disconnect(this.username);
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.isVideoCallActive) {
      this.webRTCService.stopCall();
    }
    this.authService.logout();
  }

}
