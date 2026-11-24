const API = {
    async get(url) {
        try {
            const res = await fetch(url);
            if (!res.ok) {
                const err = await res.json().catch(() => ({ error: res.statusText }));
                throw new Error(err.error || `HTTP ${res.status}`);
            }
            return await res.json();
        } catch (e) {
            console.error("API GET Error:", url, e);
            throw e;
        }
    },

    async post(url, data = {}) {
        try {
            const res = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({ error: res.statusText }));
                throw new Error(err.error || `HTTP ${res.status}`);
            }
            return await res.json();
        } catch (e) {
            console.error("API POST Error:", url, e);
            throw e;
        }
    }
};
window.API = API;

const ApiClient = {
    getState: () => API.get('/api/state'),
    startSimulation: () => API.post('/api/sim/start'),
    pauseSimulation: () => API.post('/api/sim/pause'),
    resumeSimulation: () => API.post('/api/sim/resume'),
    stepSimulation: () => API.post('/api/sim/step'),
    getStory: () => API.get('/api/story'),
    submitChoice: (choiceId, optionId) => API.post(`/api/choices/${choiceId}`, { optionId: String(optionId) }),
    getSurvivors: () => API.get('/api/survivors'),
    getSurvivor: (id) => API.get(`/api/survivors/${id}`),
    getRecords: () => API.get('/api/records'),
    approveIntervention: (id) => API.post(`/api/interventions/${id}/approve`),
    rejectIntervention: (id) => API.post(`/api/interventions/${id}/reject`),
    getMetrics: () => API.get('/api/metrics'),
};
window.ApiClient = ApiClient;
