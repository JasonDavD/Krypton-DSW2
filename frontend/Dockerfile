# ---- build (Vite) ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# La URL del gateway se HORNEA en el build (Vite reemplaza import.meta.env.VITE_API_BASE_URL
# en build-time). El browser del cliente pega al gateway por su puerto publicado (localhost:8094).
ARG VITE_API_BASE_URL=http://localhost:8094
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---- serve (nginx) ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
