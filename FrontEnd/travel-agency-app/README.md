# Vue 3 + Vite

This template should help get you started developing with Vue 3 in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

Learn more about IDE Support for Vue in the [Vue Docs Scaling up Guide](https://vuejs.org/guide/scaling-up/tooling.html#ide-support).

## Environment

Create a root `.env` file with the hotel, flight, and activity API base URLs:

```bash
VITE_HOTEL_API_BASE_URL=/api/v1
VITE_FLIGHT_API_BASE_URL=/api/v1
VITE_ACTIVITY_API_BASE_URL=/api/v1
```

The hotel, flight, and activity search services now require these variables and will surface an error in the UI if the API is unavailable or returns no results.

## Multi-tenant White-label by Subdomain

This app supports tenant routing by hostname:

- `agenta.local` -> `Agent_Id = 1`
- `agentb.local` -> `Agent_Id = 2`

When a booking is saved, the mapped `Agent_Id` is sent in the booking payload automatically.

### Local setup

1. Add local host entries:

```bash
sudo sh -c 'printf "\n127.0.0.1 agenta.local\n127.0.0.1 agentb.local\n" >> /etc/hosts'
```

2. Start the app:

```bash
npm run dev
```

3. Open either tenant URL:

- `http://agenta.local:5173`
- `http://agentb.local:5173`

Optional override for tenant selection (useful for QA):

```bash
VITE_TENANT=agentb npm run dev
```
