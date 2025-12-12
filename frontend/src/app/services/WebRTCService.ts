import { Injectable } from '@angular/core';
import { WebSocketService } from './WebSocketService';

@Injectable({
  providedIn: 'root'
})
export class WebRTCService {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  
  private configuration: RTCConfiguration = {
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' }
    ]
  };

  constructor(private wsService: WebSocketService) {
    this.setupEventListeners();
  }

  private setupEventListeners(): void {
    window.addEventListener('videoSignal', (event: any) => {
      const { type, data } = event.detail;
      
      switch(type) {
        case 'offer':
          this.handleOffer(data);
          break;
        case 'answer':
          this.handleAnswer(data);
          break;
        case 'ice':
          this.handleIceCandidate(data);
          break;
      }
    });
  }

  async startLocalStream(videoElement: HTMLVideoElement): Promise<void> {
    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true
      });
      
      videoElement.srcObject = this.localStream;
      videoElement.muted = true; // Éviter le feedback audio
    } catch (error) {
      console.error('Erreur accès média:', error);
      throw error;
    }
  }

  async initiateCall(remotePeer: string): Promise<void> {
    this.createPeerConnection(remotePeer);
    
    // Ajouter les tracks locaux
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => {
        this.peerConnection?.addTrack(track, this.localStream!);
      });
    }

    // Créer et envoyer l'offre
    const offer = await this.peerConnection!.createOffer();
    await this.peerConnection!.setLocalDescription(offer);
    
    this.wsService.sendVideoOffer(remotePeer, offer);
  }

  private createPeerConnection(remotePeer: string): void {
    this.peerConnection = new RTCPeerConnection(this.configuration);

    // Gérer les ICE candidates
    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        this.wsService.sendIceCandidate(remotePeer, event.candidate);
      }
    };

    // Gérer le stream distant
    this.peerConnection.ontrack = (event) => {
      if (!this.remoteStream) {
        this.remoteStream = new MediaStream();
      }
      this.remoteStream.addTrack(event.track);
      
      // Émission d'événement pour mettre à jour la vidéo distante
      window.dispatchEvent(new CustomEvent('remoteStream', {
        detail: this.remoteStream
      }));
    };

    // Surveiller l'état de la connexion
    this.peerConnection.onconnectionstatechange = () => {
      console.log('État connexion:', this.peerConnection?.connectionState);
    };
  }

  private async handleOffer(data: any): Promise<void> {
    this.createPeerConnection(data.from);
    
    // Ajouter les tracks locaux
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => {
        this.peerConnection?.addTrack(track, this.localStream!);
      });
    }

    await this.peerConnection!.setRemoteDescription(
      new RTCSessionDescription(data.signal)
    );

    const answer = await this.peerConnection!.createAnswer();
    await this.peerConnection!.setLocalDescription(answer);
    
    this.wsService.sendVideoAnswer(data.from, answer);
  }

  private async handleAnswer(data: any): Promise<void> {
    await this.peerConnection?.setRemoteDescription(
      new RTCSessionDescription(data.signal)
    );
  }

  private async handleIceCandidate(data: any): Promise<void> {
    if (data.signal) {
      await this.peerConnection?.addIceCandidate(
        new RTCIceCandidate(data.signal)
      );
    }
  }

  stopCall(): void {
    // Arrêter tous les tracks locaux
    this.localStream?.getTracks().forEach(track => track.stop());
    
    // Fermer la connexion peer
    this.peerConnection?.close();
    
    this.localStream = null;
    this.remoteStream = null;
    this.peerConnection = null;
  }

  toggleAudio(enabled: boolean): void {
    this.localStream?.getAudioTracks().forEach(track => {
      track.enabled = enabled;
    });
  }

  toggleVideo(enabled: boolean): void {
    this.localStream?.getVideoTracks().forEach(track => {
      track.enabled = enabled;
    });
  }
}