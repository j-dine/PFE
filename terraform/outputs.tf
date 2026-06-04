output "instance_public_ip" {
  description = "L'adresse IP publique du serveur EC2."
  value       = aws_instance.app_server.public_ip
}

output "instance_public_dns" {
  description = "L'adresse DNS publique du serveur EC2."
  value       = aws_instance.app_server.public_dns
}

output "ssh_connection_command" {
  description = "La commande pour se connecter en SSH au serveur."
  value       = "ssh -i labsuser.pem ubuntu@${aws_instance.app_server.public_ip}"
}
