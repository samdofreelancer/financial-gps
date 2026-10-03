FROM mcr.microsoft.com/playwright:v1.63.0-noble

# Allure's report generator runs on the JVM; the Playwright base image does not include Java.
RUN apt-get update \
	&& apt-get install -y --no-install-recommends default-jre-headless \
	&& rm -rf /var/lib/apt/lists/*

WORKDIR /workspace/e2e
COPY package*.json ./
RUN npm ci
COPY . .

CMD ["npm", "test"]
