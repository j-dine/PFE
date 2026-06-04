# Recherche du VPC par défaut existant sur le compte AWS Academy
data "aws_vpc" "default" {
  default = true
}

# Recherche des sous-réseaux associés au VPC par défaut
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Recherche de l'AMI la plus récente pour Ubuntu 22.04 LTS
data "aws_ami" "ubuntu" {
  most_recent = true
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
  owners = ["099720109477"] # Identifiant de Canonical (éditeur d'Ubuntu)
}

# Création du Groupe de Sécurité
resource "aws_security_group" "app_sg" {
  name        = "pfe-app-security-group"
  description = "Groupe de securite pour le serveur PFE (SSH, HTTP, Gateway, Frontend)"
  vpc_id      = data.aws_vpc.default.id

  # Autoriser SSH (Port 22)
  ingress {
    description      = "Acces SSH"
    from_port        = 22
    to_port          = 22
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  # Autoriser HTTP (Port 80)
  ingress {
    description      = "Acces HTTP pour le Frontend"
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  # Autoriser HTTPS (Port 443)
  ingress {
    description      = "Acces HTTPS securise"
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  # Autoriser l'API Gateway en direct si nécessaire (Port 8080)
  ingress {
    description      = "Acces API Gateway en direct"
    from_port        = 8080
    to_port          = 8080
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  # Autoriser le port par défaut du conteneur frontend s'il est mappé en dehors de 80 (Port 6745)
  ingress {
    description      = "Acces direct au conteneur Frontend"
    from_port        = 6745
    to_port          = 6745
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  # Autoriser tout le trafic sortant (Egress)
  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "pfe-security-group"
  }
}

# Création de l'instance EC2
resource "aws_instance" "app_server" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.instance_type
  key_name      = var.key_name

  # Choix du sous-réseau par défaut (le premier trouvé dans la liste)
  subnet_id = data.aws_subnets.default.ids[0]

  # Associer le groupe de sécurité créé
  vpc_security_group_ids = [aws_security_group.app_sg.id]

  # Profil d'instance obligatoire pour AWS Academy (LabInstanceProfile pré-existant)
  iam_instance_profile = "LabInstanceProfile"

  # Configurer un disque SSD gp3 de taille variable
  root_block_device {
    volume_size           = var.volume_size
    volume_type           = "gp3"
    delete_on_termination = true
  }

  # Script pour installer automatiquement Docker et Docker Compose
  user_data = file("${path.module}/scripts/install_docker.sh")

  tags = {
    Name = "PFE-App-Server"
  }
}
