import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ChatMessage {
  id?: number;
  content: string;
  sender: string;
  roomId: string;
  type: string;
  timestamp?: Date;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private client: Client;
  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  public messages$ = this.messagesSubject.asObservable();
  
  private connectedSubject = new BehaviorSubject<boolean>(false);
  public connected$ = this.connectedSubject.asObservable();

  constructor() {
    this.client = new Client();
  }

  connect(username: string): void {
    // Récupérer le token JWT
    const token = localStorage.getItem('auth_token');
    
    if (!token) {
      console.error('No JWT token found, cannot connect to WebSocket');
      return;
    }

    console.log('Connecting to WebSocket for user:', username);
    console.log('Token présent:', token.substring(0, 20) + '...');

    this.client.webSocketFactory = () => {
      return new SockJS('http://localhost:8080/ws');
    };

    // IMPORTANT: Ajouter le token dans les headers STOMP
    this.client.connectHeaders = {
      'Authorization': `Bearer ${token}`
    };

    this.client.onConnect = () => {
      console.log('✅ Connected to WebSocket with authentication');
      this.connectedSubject.next(true);

      // S'abonner aux messages de chat
      this.client.subscribe('/topic/messages', (message: IMessage) => {
        const chatMessage = JSON.parse(message.body);
        const currentMessages = this.messagesSubject.value;
        this.messagesSubject.next([...currentMessages, chatMessage]);
      });

      // S'abonner aux notifications
      this.client.subscribe('/topic/notifications', (message: IMessage) => {
        console.log('Notification:', message.body);
      });

      // Messages privés pour la visio
      this.client.subscribe(`/user/queue/video.offer`, (message: IMessage) => {
        this.handleVideoSignal('offer', JSON.parse(message.body));
      });

      this.client.subscribe(`/user/queue/video.answer`, (message: IMessage) => {
        this.handleVideoSignal('answer', JSON.parse(message.body));
      });

      this.client.subscribe(`/user/queue/video.ice`, (message: IMessage) => {
        this.handleVideoSignal('ice', JSON.parse(message.body));
      });

      this.client.subscribe(`/user/queue/video.call`, (message: IMessage) => {
        this.handleIncomingCall(JSON.parse(message.body));
      });

      // Envoyer notification de connexion
      this.sendMessage({
        content: `${username} a rejoint le chat`,
        sender: username,
        roomId: 'general',
        type: 'JOIN'
      });
    };

    this.client.onDisconnect = () => {
      console.log('Disconnected from WebSocket');
      this.connectedSubject.next(false);
    };

    this.client.onStompError = (frame) => {
      console.error('WebSocket error:', frame.headers['message']);
      console.error('Details:', frame.body);
      this.connectedSubject.next(false);
    };

    this.client.activate();
  }

  sendMessage(message: ChatMessage): void {
    if (this.client.connected) {
      this.client.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message)
      });
    }
  }

  // Méthodes pour la signalisation WebRTC
  sendVideoOffer(to: string, offer: RTCSessionDescriptionInit): void {
    this.client.publish({
      destination: '/app/video.offer',
      body: JSON.stringify({ to, signal: offer, type: 'OFFER' })
    });
  }

  sendVideoAnswer(to: string, answer: RTCSessionDescriptionInit): void {
    this.client.publish({
      destination: '/app/video.answer',
      body: JSON.stringify({ to, signal: answer, type: 'ANSWER' })
    });
  }

  sendIceCandidate(to: string, candidate: RTCIceCandidate): void {
    this.client.publish({
      destination: '/app/video.ice',
      body: JSON.stringify({ to, signal: candidate, type: 'ICE_CANDIDATE' })
    });
  }

  initiateCall(to: string, roomId: string): void {
    this.client.publish({
      destination: '/app/video.call',
      body: JSON.stringify({ to, roomId, type: 'CALL_REQUEST' })
    });
  }

  private handleVideoSignal(type: string, data: any): void {
    // Émetteur d'événements pour le composant vidéo
    window.dispatchEvent(new CustomEvent('videoSignal', { 
      detail: { type, data } 
    }));
  }

  private handleIncomingCall(data: any): void {
    window.dispatchEvent(new CustomEvent('incomingCall', { 
      detail: data 
    }));
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
    }
  }
}