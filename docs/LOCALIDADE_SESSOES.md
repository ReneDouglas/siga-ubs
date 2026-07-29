# Localidade aproximada das sessões

As telas de sessões de `ADMIN` e `SMS` exibem a **Localidade no login**. A
informação é obtida uma única vez, durante a autenticação, e permanece somente
como atributo da Spring Session. Não há migração nem tabela adicional.

## Produção

1. No painel da zona `sigaubs.com.br` na Cloudflare, acesse **Rules**,
   **Transform Rules**, **Managed Transforms**.
2. Em transformações de cabeçalhos de requisição, habilite
   **Add visitor location headers**.
3. Mantenha os registros DNS dos hosts da aplicação com proxy da Cloudflare
   habilitado.
4. Mantenha a origem restrita aos ranges oficiais da Cloudflare. O
   `nginx/prd.conf` rejeita conexões que não venham desses ranges; a restrição
   equivalente também deve existir no firewall da VPS.

O nginx converte `CF-IPCity`, `CF-Region-Code`, `CF-Region` e `CF-IPCountry`
para cabeçalhos internos sobrescritos pelo gateway. Latitude, longitude,
continente, CEP, metro e fuso horário são descartados antes do encaminhamento
à aplicação.

A configuração `sigaubs.security.session.location-source=trusted-proxy` faz a
aplicação confiar exclusivamente nesses cabeçalhos internos. Não habilite esse
modo se a aplicação puder ser acessada diretamente, sem o gateway.

## Desenvolvimento

O perfil `dev` usa:

```properties
sigaubs.security.session.location-source=local
```

Por isso, novos logins mostram `Rede local · Desenvolvimento`. O nginx local
sobrescreve com vazio qualquer cabeçalho de localização enviado pelo cliente.

## Comportamento e privacidade

- É armazenada somente uma descrição reduzida, como
  `Afogados da Ingazeira, PE · BR`.
- IP bruto, coordenadas e CEP não são armazenados nesse metadado.
- A localidade é aproximada e pode representar a saída de uma VPN, rede móvel
  ou rede Tor; não deve ser usada isoladamente para bloquear ou autorizar.
- A descrição representa o local do login, não a posição atual do usuário.
- Sessões anteriores à implantação mostram `Localidade indisponível`.
- Revogação ou expiração remove a localidade junto com a sessão.

Após habilitar a transformação e implantar a configuração, encerre a sessão de
teste e autentique novamente. Sessões já existentes não podem ser preenchidas
retroativamente porque o sistema não armazenava seu IP de origem.

Referências:

- <https://developers.cloudflare.com/network/ip-geolocation/>
- <https://developers.cloudflare.com/rules/transform/managed-transforms/reference/>
