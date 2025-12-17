import { Injectable } from '@angular/core';
import { WebSocketService } from './WebSocketService';

interface PendingSignal {
  type: string;
  data: any;
}

@Injectable({
  providedIn: 'root'
})
export class WebRTCService {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  
  // ✅ NOUVEAU: File d'attente pour les signaux reçus avant d'être prêt
  private pendingSignals: PendingSignal[] = [];
  private isReady: boolean = false;
  
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
      
      // ✅ Si pas prêt, mettre en attente
      if (!this.isReady && type === 'offer') {
        console.log('⏳ Signal reçu trop tôt, mise en attente:', type);
        this.pendingSignals.push({ type, data });
        return;
      }
      
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
      console.log('🎥 Demande accès caméra/micro...');
      
      if (!videoElement) {
        throw new Error('Élément vidéo non trouvé');
      }

      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 1280 },
          height: { ideal: 720 }
        },
        audio: true
      });
      
      console.log('✅ Accès média accordé');
      
      videoElement.srcObject = this.localStream;
      videoElement.muted = true;
      
      try {
        await videoElement.play();
        console.log('✅ Vidéo locale démarrée');
      } catch (playError) {
        console.warn('Autoplay bloqué:', playError);
      }

      // ✅ Marquer comme prêt et traiter les signaux en attente
      this.isReady = true;
      this.processPendingSignals();
      
    } catch (error: any) {
      console.error('❌ Erreur accès média:', error);
      
      if (error.name === 'NotAllowedError') {
        throw new Error('Permission refusée. Veuillez autoriser l\'accès à la caméra et au microphone.');
      } else if (error.name === 'NotFoundError') {
        throw new Error('Aucune caméra ou microphone trouvé sur cet appareil.');
      } else if (error.name === 'NotReadableError') {
        throw new Error('Caméra/microphone déjà utilisé par une autre application.');
      } else if (error.name === 'OverconstrainedError') {
        throw new Error('Les contraintes vidéo ne peuvent pas être satisfaites.');
      } else {
        throw new Error(`Erreur d'accès média: ${error.message}`);
      }
    }
  }

  // ✅ Traiter les signaux en attente
  private processPendingSignals(): void {
    if (this.pendingSignals.length > 0) {
      console.log('🔄 Traitement des', this.pendingSignals.length, 'signaux en attente');
      
      this.pendingSignals.forEach(signal => {
        switch(signal.type) {
          case 'offer':
            this.handleOffer(signal.data);
            break;
          case 'answer':
            this.handleAnswer(signal.data);
            break;
          case 'ice':
            this.handleIceCandidate(signal.data);
            break;
        }
      });
      
      this.pendingSignals = [];
    }
  }

  async initiateCall(remotePeer: string): Promise<void> {
    if (!this.localStream) {
      throw new Error('Stream local non initialisé');
    }

    this.createPeerConnection(remotePeer);
    
    this.localStream.getTracks().forEach(track => {
      console.log('➕ Ajout track:', track.kind);
      this.peerConnection?.addTrack(track, this.localStream!);
    });

    const offer = await this.peerConnection!.createOffer();
    await this.peerConnection!.setLocalDescription(offer);
    
    console.log('📤 Envoi offre WebRTC à', remotePeer);
    this.wsService.sendVideoOffer(remotePeer, offer);
  }

  private createPeerConnection(remotePeer: string): void {
    console.log('🔗 Création PeerConnection pour', remotePeer);
    this.peerConnection = new RTCPeerConnection(this.configuration);

    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        console.log('🧊 ICE candidate:', event.candidate.candidate.substring(0, 50) + '...');
        this.wsService.sendIceCandidate(remotePeer, event.candidate);
      }
    };

    this.peerConnection.ontrack = (event) => {
      console.log('📹 Track distant reçu:', event.track.kind);
      
      if (!this.remoteStream) {
        this.remoteStream = new MediaStream();
      }
      this.remoteStream.addTrack(event.track);
      
      window.dispatchEvent(new CustomEvent('remoteStream', {
        detail: this.remoteStream
      }));
    };

    this.peerConnection.onconnectionstatechange = () => {
      console.log('🔄 État connexion:', this.peerConnection?.connectionState);
    };

    this.peerConnection.oniceconnectionstatechange = () => {
      console.log('🧊 État ICE:', this.peerConnection?.iceConnectionState);
    };
  }

  private async handleOffer(data: any): Promise<void> {
    console.log('📥 Réception offre de', data.from);
    
    if (!this.localStream) {
      console.error('❌ Pas de stream local pour répondre à l\'offre');
      return;
    }

    this.createPeerConnection(data.from);
    
    this.localStream.getTracks().forEach(track => {
      this.peerConnection?.addTrack(track, this.localStream!);
    });

    await this.peerConnection!.setRemoteDescription(
      new RTCSessionDescription(data.signal)
    );

    const answer = await this.peerConnection!.createAnswer();
    await this.peerConnection!.setLocalDescription(answer);
    
    console.log('📤 Envoi réponse à', data.from);
    this.wsService.sendVideoAnswer(data.from, answer);
  }

  private async handleAnswer(data: any): Promise<void> {
    console.log('📥 Réception réponse de', data.from);
    
    if (!this.peerConnection) {
      console.error('❌ Pas de peerConnection pour traiter la réponse');
      return;
    }

    await this.peerConnection.setRemoteDescription(
      new RTCSessionDescription(data.signal)
    );
  }

  private async handleIceCandidate(data: any): Promise<void> {
    console.log('🧊 Réception ICE candidate');
    
    if (!this.peerConnection) {
      console.warn('⏳ PeerConnection pas prête, ICE candidate ignoré');
      return;
    }

    if (data.signal) {
      try {
        await this.peerConnection.addIceCandidate(
          new RTCIceCandidate(data.signal)
        );
      } catch (error) {
        console.error('❌ Erreur ajout ICE candidate:', error);
      }
    }
  }

  stopCall(): void {
    console.log('🛑 Arrêt de l\'appel');
    
    this.localStream?.getTracks().forEach(track => {
      track.stop();
      console.log('⏹️ Track arrêté:', track.kind);
    });
    
    this.peerConnection?.close();
    
    this.localStream = null;
    this.remoteStream = null;
    this.peerConnection = null;
    this.isReady = false;
    this.pendingSignals = [];
  }

  toggleAudio(enabled: boolean): void {
    this.localStream?.getAudioTracks().forEach(track => {
      track.enabled = enabled;
      console.log('🎤 Audio:', enabled ? 'activé' : 'désactivé');
    });
  }

  toggleVideo(enabled: boolean): void {
    this.localStream?.getVideoTracks().forEach(track => {
      track.enabled = enabled;
      console.log('📹 Vidéo:', enabled ? 'activée' : 'désactivée');
    });
  }
}
