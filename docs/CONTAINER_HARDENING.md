# Hardening dos containers

Os arquivos Compose aplicam o perfil padrão `docker-default` de seccomp/AppArmor
do host, `no-new-privileges`, remoção de capabilities e filesystem somente
leitura onde o processo suporta. Diretórios temporários e de runtime são
declarados por `tmpfs` ou volume específico.

As redes são segmentadas:

- `edge`: somente o gateway;
- `application`: gateway e aplicação;
- `database`: aplicação e MySQL.

O gateway não participa da rede do banco. Em desenvolvimento, somente ele
publica portas. Em produção, o MySQL publica `3306` exclusivamente em
`127.0.0.1`, permitindo acesso administrativo por túnel SSH sem exposição na
interface pública. A porta de gestão `9090` existe apenas na rede interna da
aplicação.

O nginx executa diretamente como UID/GID `101`. Os `tmpfs` de
`/var/cache/nginx`, `/run` e `/tmp` pertencem a esse usuário; isso preserva o
filesystem raiz read-only sem impedir a criação dos arquivos temporários e do
PID.

Exemplo de túnel, usando `3307` na máquina administrativa:

```bash
ssh -L 3307:127.0.0.1:3306 usuario@vps
```

No DBeaver, conectar em `localhost:3307`. O `read_only: true` protege o
filesystem raiz do container; o volume `/var/lib/mysql` continua gravável e
não limita comandos SQL autorizados ao usuário do banco.

Antes do deploy, validar que o host mantém o perfil `docker-default` habilitado.
Caso exista um perfil AppArmor próprio, ele deve permitir apenas leitura do JAR,
certificados e configuração nginx, escrita nos `tmpfs`/volumes declarados e as
conexões previstas entre as redes acima. Uma exceção deve ser documentada se o
runtime do provedor não oferecer AppArmor; não desabilitar seccomp como
compensação.
