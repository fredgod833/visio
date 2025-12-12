import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
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

  messages: ChatMessage[] = [];
  newMessage: string = '';
  username: string = '';
  roomId: string = 'general';

  isConnected: boolean = false;
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
    private router: Router
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
    console.log('Chat initialisé pour:', this.username);

    // Se connecter au WebSocket avec le token
    this.wsService.connect(this.username);

    // S'abonner aux messages
    this.subscriptions.push(
      this.wsService.messages$.subscribe(messages => {
        this.messages = messages;
        this.scrollToBottom();
      })
    );

    // S'abonner à l'état de connexion
    this.subscriptions.push(
      this.wsService.connected$.subscribe(connected => {
        this.isConnected = connected;
      })
    );

    // Écouter les appels entrants
    window.addEventListener('incomingCall', this.handleIncomingCall.bind(this));

    // Écouter le stream distant
    window.addEventListener('remoteStream', this.handleRemoteStream.bind(this));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.wsService.disconnect();
    if (this.isVideoCallActive) {
      this.webRTCService.stopCall();
    }
  }

  sendMessage(): void {
    if (this.newMessage.trim() && this.isConnected) {
      const message: ChatMessage = {
        content: this.newMessage,
        sender: this.username,
        roomId: this.roomId,
        type: 'CHAT',
        timestamp: new Date()
      };

      this.wsService.sendMessage(message);
      this.newMessage = '';
    }
  }

  async startVideoCall(targetUsername: string): Promise<void> {
    this.remotePeerUsername = targetUsername;
    this.isVideoCallActive = true;

    try {
      // Démarrer le stream local
      await this.webRTCService.startLocalStream(
        this.localVideo.nativeElement
      );

      // Initier l'appel
      this.wsService.initiateCall(targetUsername, this.roomId);

      // Créer l'offre WebRTC
      await this.webRTCService.initiateCall(targetUsername);
    } catch (error) {
      console.error('Erreur démarrage appel vidéo:', error);
      this.isVideoCallActive = false;
    }
  }

  async handleIncomingCall(event: any): Promise<void> {
    const { from } = event.detail;
    this.incomingCallFrom = from;
    this.showVideoCallDialog = true;
  }

  async acceptCall(): Promise<void> {
    this.showVideoCallDialog = false;
    this.remotePeerUsername = this.incomingCallFrom;
    this.isVideoCallActive = true;

    try {
      await this.webRTCService.startLocalStream(
        this.localVideo.nativeElement
      );
    } catch (error) {
      console.error('Erreur acceptation appel:', error);
      this.isVideoCallActive = false;
    }
  }

  rejectCall(): void {
    this.showVideoCallDialog = false;
    this.incomingCallFrom = '';
  }

  handleRemoteStream(event: any): void {
    const stream = event.detail;
    this.remoteVideo.nativeElement.srcObject = stream;
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
    this.authService.logout();
    this.wsService.disconnect();
    this.router.navigate(['/login']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }
}
