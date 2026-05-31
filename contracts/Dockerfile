FROM node:20-alpine

RUN apk add --no-cache netcat-openbsd

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .

# Compile contracts at build time so the image is ready to deploy immediately
RUN npx hardhat compile

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
