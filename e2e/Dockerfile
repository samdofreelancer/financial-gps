FROM mcr.microsoft.com/playwright:v1.63.0-noble

WORKDIR /workspace/e2e
COPY package*.json ./
RUN npm ci
COPY . .

CMD ["npm", "test"]
