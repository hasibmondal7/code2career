FROM node:24-alpine

RUN apk add --no-cache docker-cli
WORKDIR /app
COPY package.json server.js ./

EXPOSE 8090
CMD ["npm", "start"]
