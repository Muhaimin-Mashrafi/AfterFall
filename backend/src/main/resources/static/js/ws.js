/* Lightweight self-contained SockJS/STOMP bridge client for offline local frontend */
class AfterFallWS {
    constructor(endpoint, onConnect, onDisconnect) {
        this.endpoint = endpoint;
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
        this.subscriptions = new Map();
        this.socket = null;
        this.connected = false;
        this.reconnectAttempts = 0;
    }

    connect() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}${this.endpoint}/websocket`;
        
        try {
            this.socket = new WebSocket(wsUrl);
            this.socket.onopen = () => {
                this.connected = true;
                this.reconnectAttempts = 0;
                // Send STOMP connect frame
                this.socket.send("CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:10000,10000\n\n\0");
            };

            this.socket.onmessage = (event) => {
                const text = event.data;
                if (text.startsWith("CONNECTED")) {
                    if (this.onConnect) this.onConnect();
                    // Resubscribe active topics
                    this.subscriptions.forEach((callback, topic) => {
                        this.sendSubscribe(topic);
                    });
                } else if (text.startsWith("MESSAGE")) {
                    const bodyIndex = text.indexOf("\n\n");
                    if (bodyIndex !== -1) {
                        const body = text.substring(bodyIndex + 2).replace(/\0$/, '');
                        const headers = text.substring(0, bodyIndex).split("\n");
                        let destination = "";
                        for (const h of headers) {
                            if (h.startsWith("destination:")) {
                                destination = h.substring("destination:".length).trim();
                            }
                        }
                        if (destination && this.subscriptions.has(destination)) {
                            try {
                                const parsed = JSON.parse(body);
                                this.subscriptions.get(destination)(parsed);
                            } catch (e) {
                                this.subscriptions.get(destination)(body);
                            }
                        }
                    }
                }
            };

            this.socket.onclose = () => {
                this.connected = false;
                if (this.onDisconnect) this.onDisconnect();
                this.scheduleReconnect();
            };

            this.socket.onerror = () => {
                this.connected = false;
            };
        } catch (e) {
            this.connected = false;
            this.scheduleReconnect();
        }
    }

    scheduleReconnect() {
        const delay = Math.min(5000, 1000 * Math.pow(1.5, this.reconnectAttempts++));
        setTimeout(() => this.connect(), delay);
    }

    subscribe(topic, callback) {
        this.subscriptions.set(topic, callback);
        if (this.connected) {
            this.sendSubscribe(topic);
        }
    }

    sendSubscribe(topic) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            const frame = `SUBSCRIBE\nid:sub-${topic}\ndestination:${topic}\n\n\0`;
            this.socket.send(frame);
        }
    }
}
window.AfterFallWS = AfterFallWS;

let defaultWsClient = null;
const WsClient = {
    connect: (opts = {}) => {
        defaultWsClient = new AfterFallWS('/ws', opts.onConnect, opts.onDisconnect);
        defaultWsClient.connect();
        return defaultWsClient;
    },
    subscribe: (topic, callback) => {
        if (!defaultWsClient) {
            defaultWsClient = new AfterFallWS('/ws');
            defaultWsClient.connect();
        }
        defaultWsClient.subscribe(topic, callback);
    }
};
window.WsClient = WsClient;
