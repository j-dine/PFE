variable "aws_region" {
  description = "La région AWS pour le déploiement. Par défaut us-east-1 (région de l'AWS Academy)."
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "Le type d'instance EC2. t3.medium (4 Go RAM) ou t3.large (8 Go RAM) sont recommandés pour faire tourner tous les microservices et Postgres."
  type        = string
  default     = "t3.medium"
}

variable "key_name" {
  description = "Le nom de la paire de clés SSH existante dans AWS. Dans AWS Academy, la clé pré-créée s'appelle généralement 'vockey'."
  type        = string
  default     = "vockey"
}

variable "volume_size" {
  description = "Taille du disque dur de l'instance EC2 (en Go)."
  type        = number
  default     = 20
}
