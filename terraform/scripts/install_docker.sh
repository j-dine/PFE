#!/bin/bash
# Script d'installation de Docker et Docker Compose pour Ubuntu 22.04 LTS

# Mettre à jour les paquets
sudo apt-get update -y

# Installer les dépendances
sudo apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    software-properties-common \
    gnupg \
    lsb-release \
    git \
    unzip

# Ajouter la clé GPG officielle de Docker
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Configurer le dépôt stable de Docker
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Mettre à jour l'index des paquets
sudo apt-get update -y

# Installer Docker Engine, CLI et le plugin Docker Compose
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Démarrer Docker et s'assurer qu'il se lance au démarrage
sudo systemctl enable docker
sudo systemctl start docker

# Ajouter l'utilisateur ubuntu au groupe docker pour exécuter docker sans sudo
sudo usermod -aG docker ubuntu

echo "Docker et Docker Compose installés avec succès !"
